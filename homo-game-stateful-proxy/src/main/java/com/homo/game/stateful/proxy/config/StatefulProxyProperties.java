package com.homo.game.stateful.proxy.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@Slf4j
@Data
public class StatefulProxyProperties {
    @Value("${sm.stateful.server.open:true}")
    private Boolean serverOpen;

    @Value("#{'${sm.stateful.server.whitelist:123}'.split(',')}")
    private Set<String> serverWhitelist;

    @Value("${sm.stateful.server.close.info}")
    private String closeInfo;
    @Value("${sm.stateful.client.offline.delay.second:10}")
    private Integer clientOfflineDelayCloseSecond;
    @Value("#{'${sm.stateful.client.offline.notify.services:''}'.split(',')}")
    private Set<String> clientOfflineNotifyServiceSet;
}
