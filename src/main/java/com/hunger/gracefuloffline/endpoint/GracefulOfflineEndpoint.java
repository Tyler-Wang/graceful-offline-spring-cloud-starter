package com.hunger.gracefuloffline.endpoint;


import com.hunger.gracefuloffline.handler.CheckInstanceInRibbonHandler;
import com.hunger.gracefuloffline.handler.GracefulOffineHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "endpoints.graceful.offline")
public class GracefulOfflineEndpoint extends AbstractEndpoint<Map<String, Object>> {
    
    private static final Logger logger = LoggerFactory.getLogger(GracefulOfflineEndpoint.class);
    
    public static final String ID = "gracefuloffline";
    
    private CheckInstanceInRibbonHandler checkInstanceInRibbonHandler;
    
    private GracefulOffineHandler gracefulOffineHandler;
    
    public GracefulOfflineEndpoint(CheckInstanceInRibbonHandler checkInstanceInRibbonHandler, GracefulOffineHandler gracefulOffineHandler){
        super(ID);
        this.checkInstanceInRibbonHandler = checkInstanceInRibbonHandler;
        this.gracefulOffineHandler = gracefulOffineHandler;
    }
    
    public Map<String, Object> invoke() {
        return gracefulOffineHandler.shutdown();
    }

    /**
     * 判断服务实例是否在本服务ribbon列表中
     * @param serverName
     * @param instanceId
     * @return
     */
    public Boolean checkInstance(String serverName, String instanceId){
        return checkInstanceInRibbonHandler.check(serverName, instanceId);
    }
    
    public Boolean unregister(){
        return gracefulOffineHandler.unregister();
    }
}
