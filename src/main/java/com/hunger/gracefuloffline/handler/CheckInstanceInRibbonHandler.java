package com.hunger.gracefuloffline.handler;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CheckInstanceInRibbonHandler {

    private static final Logger logger = LoggerFactory.getLogger(CheckInstanceInRibbonHandler.class);
    
    private SpringClientFactory springClientFactory;
    
    public CheckInstanceInRibbonHandler(SpringClientFactory springClientFactory){
        this.springClientFactory = springClientFactory;
    }

    /**
     * 检查指定服务的指定实例是否不在 ribbon的服务列表中
     * @param serverName
     * @param instanceId
     * @return
     */
    public boolean check(String serverName, String instanceId){
        ConcurrentHashMap<String, AnnotationConfigApplicationContext> contextMap = this.ribbonContext();
        if(contextMap == null){
            logger.debug("cannot find ribbon context");
            return true;
        }
        
        List<Server> serverList = this.findServerList(contextMap, serverName);
        if(serverList.isEmpty()){
            logger.debug("cannot find any {} server", serverName);
            return true;
        }
        
        Server server = this.findServer(serverList, instanceId);
        if(server == null){
            logger.debug("cannot find {} {} instance", serverName, instanceId);
            return true;
        }else{
            return !server.isAlive();
        }
    }

    /**
     * 获取ribbon加载的context
     * @return
     */
    private ConcurrentHashMap<String, AnnotationConfigApplicationContext> ribbonContext(){
        Field field = ReflectionUtils.findField(springClientFactory.getClass(), "contexts");
        field.setAccessible(true);
        ConcurrentHashMap<String, AnnotationConfigApplicationContext> contexts = (ConcurrentHashMap)ReflectionUtils.getField(field, springClientFactory);
        logger.debug("ribbon contexts == null: {}", contexts == null);
        return contexts;
    }
    
    private List<Server> findServerList(ConcurrentHashMap<String, AnnotationConfigApplicationContext> contextMap, String serverName){
        List<Server> serverList = new ArrayList<>();
        AnnotationConfigApplicationContext annoContext = contextMap.get(serverName);
        if(annoContext == null){
            logger.debug("cannot find {} AnnotationConfigApplicationContext", serverName);
            return serverList;
        }

        DynamicServerListLoadBalancer balancer = annoContext.getBean(DynamicServerListLoadBalancer.class);
        if(balancer == null){
            logger.debug("cannot find {} DynamicServerListLoadBalancer", serverName);
            return serverList;
        }

        serverList = balancer.getAllServers();
        
        return serverList;
    }


    /**
     * ribbon中查找Server
     * @param serverList
     * @param instanceId
     * @return
     */
    private Server findServer(List<Server> serverList, String instanceId){
        for(Server server : serverList){
            DiscoveryEnabledServer dServer = (DiscoveryEnabledServer)server;
            InstanceInfo instanceInfo = dServer.getInstanceInfo();
            String serverInstanceId = instanceInfo.getInstanceId();
            if(serverInstanceId.equalsIgnoreCase(instanceId)){
                return server;
            }
        }
        return null;
    }
}
