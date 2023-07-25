package com.homo.game.proxy.config;


import lombok.Data;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@Data
@ToString
public class ProxyHandlerProperties {
    @Value("${homo.common.handler.server.enable:true}")
    public boolean serverEnable;
    @Value("${homo.common.handler.limit.enable:true}")
    public boolean limitEnable;
    @Value("#{'${homo.common.handler.user.whitelist}'.split(',')}")
    public Set<String> userWhiteList;
    @Value("${homo.common.handler.limit.num:10000}")
    public Integer limitNum;
}
