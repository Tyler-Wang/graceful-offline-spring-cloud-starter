package com.hunger.gracefuloffline.handler;

import com.hunger.gracefuloffline.endpoint.GracefulOfflineEndpoint;
import com.hunger.gracefuloffline.properties.CustomPath;
import com.hunger.gracefuloffline.properties.GracefulOfflineProperties;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.*;

public class GracefulOffineHandler implements ApplicationContextAware{

    private static final Map<String, Object> IN_PROCESS_MESSAGE = Collections.unmodifiableMap(Collections.singletonMap("message", "Shutting down gracefully in process, please wait"));

    private static final Map<String, Object> SHUTDOWN_MESSAGE = Collections.unmodifiableMap(Collections.singletonMap("message", "Shutting down gracefully, please check after a while"));
    
    private static final Logger logger = LoggerFactory.getLogger(GracefulOffineHandler.class);
    
    private static final String PARAM_FOMAT = "serverName=%s&instanceId=%s";
    
    private ApplicationContext applicationContext;
    
    private volatile boolean isShutdowning;
    
    private EurekaClient discoveryClient;

    private GracefulOfflineProperties properties;
    
    public GracefulOffineHandler(EurekaClient discoveryClient, GracefulOfflineProperties properties){
        this.discoveryClient = discoveryClient;
        this.properties = properties;
    }
    
    public Map<String, Object> shutdown(){
        if(this.isShutdowning){
            return IN_PROCESS_MESSAGE;
        }
        //获取当前服务信息
        InstanceInfo instanceInfo = discoveryClient.getApplicationInfoManager().getInfo();
        String appName = instanceInfo.getAppName();
        String instanceId = instanceInfo.getInstanceId();
        String serverName = appName.toLowerCase();

        List<InstanceInfo> needCheckInstances = this.filterIgnoreService(appName);
        
        String paramStr = String.format(PARAM_FOMAT, serverName, instanceId);

        List<InstanceInfo> firstFilterInstances = this.checkInstanceRibbon(needCheckInstances, paramStr);
        
        //从注册中心下线
        discoveryClient.shutdown();
        
        ShutdownAsyncTask task = new ShutdownAsyncTask(firstFilterInstances, paramStr, properties.getWait(), properties.getForce());
        Thread shutdownAsyncThread = new Thread(task, "ShutdownAsyncThread");
        shutdownAsyncThread.start();
        
        return SHUTDOWN_MESSAGE;
    }

    public Boolean unregister(){
        try{
            discoveryClient.shutdown();
            return true;
        }catch(Exception e){
            logger.error("Eureka DiscoveryClient shutdown error", e);
            return false;
        }
    }
    
    /**
     * 过滤掉当前服务实例和properties配置的ignoreService
     * @param currAppName
     * @return
     */
    private List<InstanceInfo> filterIgnoreService(String currAppName){
        List<InstanceInfo> needCheckInstances = new ArrayList<>();
        TreeSet<String> ignoreService = properties.ontainIgnoreServices();
        Applications apps = discoveryClient.getApplications();
        for(Application app : apps.getRegisteredApplications()){
            //过滤掉自身
            String lowerAppName = app.getName().toLowerCase();
            if(!lowerAppName.equalsIgnoreCase(currAppName) && !ignoreService.contains(lowerAppName)){
                needCheckInstances.addAll(app.getInstances());
            }
        }
        return needCheckInstances;
    }
    
    
    private List<InstanceInfo> checkInstanceRibbon(List<InstanceInfo> instanceInfoList, String paramStr){
        List<InstanceInfo> leftList = new ArrayList<>();
        for(InstanceInfo instance : instanceInfoList){
            String lowerAppName = instance.getAppName().toLowerCase();
            CustomPath customPath = this.properties.getAppCustomPath(lowerAppName);
            StringBuilder url = new StringBuilder();
            if(customPath != null){
                if(customPath.getManagementPort() != null && customPath.getManagementPort() != 0){
                    url = new StringBuilder("http://");
                    url.append(instance.getIPAddr()).append(":").append(customPath.getManagementPort());
                    if(customPath.getManagementContextPath() != null && customPath.getManagementContextPath().length() > 0){
                        url.append(customPath.getManagementContextPath()).append("/");
                    }
                }else{
                    url = new StringBuilder(instance.getHomePageUrl().substring(0, instance.getHomePageUrl().length()-1));
                    if(customPath.getServerContextPath() != null && customPath.getServerContextPath().length() > 0){
                        url.append(customPath.getServerContextPath());
                    }
                    if(customPath.getManagementContextPath() != null && customPath.getManagementContextPath().length() > 0){
                        url.append(customPath.getManagementContextPath());
                    }
                    url.append("/");
                }
            }else{
                url = new StringBuilder(instance.getHomePageUrl());
            }
            url.append(GracefulOfflineEndpoint.ID ).append("/check?").append(paramStr);
            boolean notExist = this.checkOneInstanceRemote(lowerAppName, url.toString());
            if(!notExist){
                leftList.add(instance);
            }
        }
        return leftList;
    }

    private boolean checkOneInstanceRemote(String appName, String url){
        try{
            RestTemplate restTemplate = new RestTemplate();
            Boolean notExist = restTemplate.execute(URI.create(url), HttpMethod.GET, null, new ResponseExtractor<Boolean>() {
                @Override
                public Boolean extractData(ClientHttpResponse clientHttpResponse) throws IOException {
                    HttpStatus httpStatus = clientHttpResponse.getStatusCode();
                    if(httpStatus.is2xxSuccessful()){
                        HttpMessageConverterExtractor messageConverterExtractor = new HttpMessageConverterExtractor(Boolean.class, restTemplate.getMessageConverters());
                        return (Boolean)messageConverterExtractor.extractData(clientHttpResponse);
                    }
                    if(httpStatus.is4xxClientError()){
                        return false;
                    }
                    if(httpStatus.is5xxServerError()){
                        return false;
                    }
                    return false;
                }
            });
            logger.info("{} {} return notExist: {}", appName, url, notExist);
            return notExist;
        }catch (Exception e){
            logger.error("{} {} {}", appName, url, e.getMessage());
            return false;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    private class ShutdownAsyncTask implements Runnable{

        boolean force; //是否强制停止
        int wait;
        List<InstanceInfo> instances;
        String paramStr;
        ShutdownAsyncTask(List<InstanceInfo> instances, String paramStr, int wait, boolean force){
            this.force = force;
            this.wait = wait;
            this.instances = instances;
            this.paramStr = paramStr;
        }

        @Override
        public void run() {
            isShutdowning = true;
            long start = System.currentTimeMillis();
            while((System.currentTimeMillis() - start)/1000 < wait){
                this.instances = checkInstanceRibbon(this.instances, this.paramStr);
                if(this.instances.size() == 0){
                    break;
                }else{
                    logger.warn("there are still some services use this service, shutdown thread will check after 5 seconds");
                    try {
                        Thread.currentThread().sleep(5000);
                    } catch (InterruptedException e) {
                        logger.error("ShutdownAsyncTask: ", e);
                    }
                }
            }
            if(this.instances.size() == 0){
                logger.info("there are no service use this service now, application will be shutdown");
                closeContext();
            }else{
                if(this.force){
                    logger.warn("although there are still some services use this service, but force is set true, application will be forced shutdown");
                    closeContext();
                }else{
                    logger.warn("after {} seconds, there are still some services use this service, application cannot be shutdown automatically. please confirm, then manually close", wait);
                }
            }
            isShutdowning = false;
        }
    }
    
    private void closeContext(){
        SpringApplication.exit(applicationContext, new ExitCodeGenerator() {
            @Override
            public int getExitCode() {
                return 0;
            }
        });
    }
    
}
