package com.hunger.gracefuloffline;

import com.hunger.gracefuloffline.endpoint.GracefulOfflineEndpoint;
import com.hunger.gracefuloffline.endpoint.mvc.GracefulOfflineMvcEndpoint;
import com.hunger.gracefuloffline.handler.CheckInstanceInRibbonHandler;
import com.hunger.gracefuloffline.handler.GracefulOffineHandler;
import com.hunger.gracefuloffline.properties.GracefulOfflineProperties;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import org.springframework.boot.actuate.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "endpoints.graceful.offline", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(GracefulOfflineProperties.class)
public class GracefulOfflineAutoConfiguration {
    
    @Bean
    @ConditionalOnClass(SpringClientFactory.class)
    @ConditionalOnEnabledEndpoint("endpoints.graceful.offline")
    public CheckInstanceInRibbonHandler checkInstanceInRibbonHandler(SpringClientFactory springClientFactory){
        return new CheckInstanceInRibbonHandler(springClientFactory);
    }

    @Bean
    @ConditionalOnClass(DiscoveryClient.class)
    @ConditionalOnEnabledEndpoint("endpoints.graceful.offline")
    public GracefulOffineHandler gracefulOffineHandler(EurekaClient discoveryClient, GracefulOfflineProperties properties){
        return new GracefulOffineHandler(discoveryClient, properties);
    }
    
    @Bean
    @ConditionalOnEnabledEndpoint("endpoints.graceful.offline")
    @ConditionalOnBean(value = {CheckInstanceInRibbonHandler.class, GracefulOffineHandler.class})
    public GracefulOfflineEndpoint endpoint(CheckInstanceInRibbonHandler checkInstanceInRibbonHandler, GracefulOffineHandler gracefulOffineHandler){
        return new GracefulOfflineEndpoint(checkInstanceInRibbonHandler, gracefulOffineHandler);
    }
    
    @Bean
    @ConditionalOnBean(GracefulOfflineEndpoint.class)
    @ConditionalOnEnabledEndpoint("endpoints.graceful.offline")
    public GracefulOfflineMvcEndpoint mvcEndpoint(GracefulOfflineEndpoint endpoint){
        return new GracefulOfflineMvcEndpoint(endpoint);
    }
}
