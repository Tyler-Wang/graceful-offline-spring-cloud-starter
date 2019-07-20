package com.hunger.gracefuloffline.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.TreeSet;

@ConfigurationProperties(prefix = "endpoints.graceful.offline")
public class GracefulOfflineProperties {

    /**
     * 检查其他服务有无本应用wait时间，默认60s
     */
    private Integer wait = 60;
    
    /**
     * 超过wait时间还有其他应用使用本服务，是否强制下线。默认不强制
     */
    private Boolean force = false;

    /**
     * 需要忽略的服务，多个逗号分隔，大小写不敏感
     */
    private String ignoreService;

    /**
     * 服务自定义的路径参数
     * server.context-path
     * management.context-path
     * management.port
     */
    private Map<String, CustomPath> customPath;


    public Integer getWait() {
        return wait;
    }

    public void setWait(Integer wait) {
        this.wait = wait;
    }

    public Boolean getForce() {
        return force;
    }

    public void setForce(Boolean force) {
        this.force = force;
    }

    public String getIgnoreService() {
        return ignoreService;
    }

    public void setIgnoreService(String ignoreService) {
        this.ignoreService = ignoreService;
    }

    public Map<String, CustomPath> getCustomPath() {
        return customPath;
    }

    public void setCustomPath(Map<String, CustomPath> customPath) {
        this.customPath = customPath;
    }
    
    public TreeSet<String> ontainIgnoreServices(){
        TreeSet<String> services = new TreeSet<>();
        if(this.ignoreService != null && this.ignoreService.length() > 0){
            String lowerIgnoreService = this.ignoreService.toLowerCase();
            String[] arr = lowerIgnoreService.split(",");
            for(String s : arr){
                services.add(s);
            }
        }
        return services;
    }
    
    public CustomPath getAppCustomPath(String appName){
        if(this.customPath == null || !this.customPath.containsKey(appName)){
            return null;
        }
        return this.customPath.get(appName);
    }
}
