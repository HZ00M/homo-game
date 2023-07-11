package com.homo.common.proxy.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

@Data
@Configuration
public class CommonProxyProperties {
    @Value("${reactor.netty.max.connection:10000}")
    private int maxConnectionCount;

    @Value("${homo.sentinel.datasource.namespace:homo_common_proxy}")
    private String datasourceNamespace;

    @Value("${homo.sentinel.datasource.flowRuleKey:flowRules}")
    private String flowRuleKey ;

    @Value("${homo.common.httpForward.flow.control.keys:{}}")
    private Set<String> flowControlKeys;

    @Value("${homo.common.httpForward.flow.control.keys:{}}")
    private Set<String> ignoreCheckTokenKeys;

    @Value("#{${homo.common.httpForward.url.map}}")
    private Map<String,String> forwardUrlMap;

    @Value("#{${homo.common.httpForward.app.checkToken.appSecretKey.map}}")
    private Map<String,String> appSecretKeyMap;

    @Value("#{${homo.common.proxy.server.port.map}}")
    private Map<String, String> serverPortMap;
}
