package com.homo.game.stateful.proxy.gate;

import com.homo.game.stateful.proxy.config.StatefulProxyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负责管理客户端连接信息
 */
@Component
@Slf4j
public class ProxyGateClientMgr {
    @Autowired
    public StatefulProxyProperties proxyProperties;
    public static Map<String, ProxyGateClient> uidToGateClientMap = new ConcurrentHashMap<>(1000);
    public static Map<String, ProxyGateClient> uidToTransferMap = new ConcurrentHashMap<>(100);

    public static ProxyGateClient putToValid(String uid, ProxyGateClient client){
        log.debug("putToValid uid {} handler {}",uid,client);
        return uidToGateClientMap.put(uid,client);
    }

    public static ProxyGateClient getFromValid(String uid){
        return uidToGateClientMap.get(uid);
    }

    public static ProxyGateClient removeFromValid(String uid){
        ProxyGateClient client = uidToGateClientMap.remove(uid);
        log.debug("removeFromValid uid {} handler {}",uid,client);
        return client;
    }


    public static ProxyGateClient putToTransferred(String uid, ProxyGateClient client){
        log.debug("putToTransferred uid {} handler {}",uid,client);
        return uidToTransferMap.put(uid,client);
    }

    public static ProxyGateClient getFromTransferred(String uid){
        return uidToTransferMap.get(uid);
    }

    public static ProxyGateClient removeFromTransferred(String uid){
        ProxyGateClient client = uidToTransferMap.remove(uid);
        log.debug("removeFromTransferred uid {} handler {}",uid,client);
        return client;
    }
}
