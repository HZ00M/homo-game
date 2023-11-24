package com.homo.game.stateful.proxy.gate;

import com.homo.game.stateful.proxy.config.StatefulProxyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负责管理可断线重连的gateClient
 */
@Component
@Slf4j
public class ProxyGateClientMgr {
    public static Map<String, ProxyGateClient> clientNameToGateClientMap = new ConcurrentHashMap<>(1000);
    public static Map<String, ProxyGateClient> uidToTransferMap = new ConcurrentHashMap<>(100);

    public static ProxyGateClient bindGate(String uid, ProxyGateClient client) {
        log.info("bindGate setUid uid {} hashCode {} client {}", uid, client.hashCode(), client);
        uidToTransferMap.put(uid,client);
        return clientNameToGateClientMap.put(client.name(), client);
    }

    public static ProxyGateClient unBindGate(String uid, ProxyGateClient client) {
        log.info("unBindGate setUid uid {} hashCode {} client {} ", uid, client.hashCode(), client);
        uidToTransferMap.remove(uid);
        return clientNameToGateClientMap.remove(client.name());
    }

    public static ProxyGateClient getClientByClientName(String clientName) {
        return clientNameToGateClientMap.get(clientName);
    }

    public static ProxyGateClient getClientByUid(String uid) {
        return uidToTransferMap.get(uid);
    }

}
