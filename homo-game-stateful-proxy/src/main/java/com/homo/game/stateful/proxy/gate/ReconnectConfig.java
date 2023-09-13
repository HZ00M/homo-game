package com.homo.game.stateful.proxy.gate;

import io.homo.proto.client.LoginAndSyncReq;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class ReconnectConfig {
    @Value("${stateful.proxy.reconnect.cache.msg.filter:}")
    private Set<String> msgFilterSet;
    @Value("${stateful.proxy.reconnect.open:true}")
    private boolean open;
    @Getter
    @Value("${stateful.proxy.reconnect.cache.max.size:500}")
    private int maxSize;
    static private final Set<String> DEFAULT_FILTER = new HashSet<>();

    static {
        DEFAULT_FILTER.add(LoginAndSyncReq.class.getSimpleName());
    }

    public boolean cacheFilter(String msgId) {
        return open && !DEFAULT_FILTER.contains(msgId) && !msgFilterSet.contains(msgId);
    }
}
