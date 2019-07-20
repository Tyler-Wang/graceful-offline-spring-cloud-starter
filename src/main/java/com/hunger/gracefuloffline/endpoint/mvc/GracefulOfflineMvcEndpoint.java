package com.hunger.gracefuloffline.endpoint.mvc;

import com.hunger.gracefuloffline.endpoint.GracefulOfflineEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;

@ConfigurationProperties(prefix = "endpoints.graceful.offline")
public class GracefulOfflineMvcEndpoint extends EndpointMvcAdapter {

    private GracefulOfflineEndpoint delegate;
    
    public GracefulOfflineMvcEndpoint(GracefulOfflineEndpoint delegate) {
        super(delegate);
        this.delegate = delegate;
    }
    
    @GetMapping(value = "/check")
    @ResponseBody
    public Boolean checkInstance(@RequestParam("serverName") String serverName, @RequestParam("instanceId") String instanceId){
        return this.delegate.checkInstance(serverName, instanceId);
    }

    @GetMapping(value = "/unregister")
    @ResponseBody
    public Boolean unregister(){
        return this.delegate.unregister();
    }
    
    @GetMapping(
            produces = {"application/vnd.spring-boot.actuator.v1+json", "application/json"}
    )
    @ResponseBody
    public Object invoke() {
        return !this.getDelegate().isEnabled() ? new ResponseEntity(Collections.singletonMap("message", "This endpoint is disabled"), HttpStatus.NOT_FOUND) : super.invoke();
    }
}
