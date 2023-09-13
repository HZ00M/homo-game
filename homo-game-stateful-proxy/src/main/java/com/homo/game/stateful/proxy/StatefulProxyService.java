package com.homo.game.stateful.proxy;

import com.homo.core.rpc.base.service.BaseService;
import com.homo.core.utils.rector.Homo;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.game.stateful.proxy.facade.IStatefulProxyService;
import com.homo.game.stateful.proxy.gate.ProxyGateClient;
import com.homo.game.stateful.proxy.gate.ProxyGateClientMgr;
import io.homo.proto.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@Slf4j
public class StatefulProxyService extends BaseService implements IStatefulProxyService {
    @Autowired
    private ProxyGateClientMgr gateClientMgr;

    @Override
    public Homo<ToClientResp> sendToClient(Integer podId, ToClientReq req) {
        String userId = req.getClientId();
        ProxyGateClient gateClient = ProxyGateClientMgr.getFromValid(userId);
        gateClient.sendToClient(req.getMsgContent().toByteArray());
        //缓存服务器发往客户端的消息，发送失败时支持重发，支持断线重连
        gateClient.cacheMsg(req.getMsgType(),req.getMsgContent().toByteArray());
        return Homo.result(true)
                .nextDo(ret -> {
                    if (ret) {
                        ToClientResp success = ToClientResp.newBuilder()
                                .setErrorCode(ToClientResp.ErrorType.OK_VALUE)
                                .setErrorMsg("ok")
                                .build();
                        return Homo.result(success);
                    } else {
                        ToClientResp error = ToClientResp.newBuilder()
                                .setErrorCode(ToClientResp.ErrorType.USER_NOT_FOUND_VALUE)
                                .setErrorMsg("not found")
                                .build();
                        return Homo.result(error);
                    }
                });
    }

    @Override
    public Homo<ToClientResp> sendToClientComplete(Integer podId, ToClientReq req) {
        String userId = req.getClientId();
        ProxyGateClient gateClient = ProxyGateClientMgr.getFromValid(userId);
        return gateClient.sendToClientComplete(req.getMsgContent().toByteArray())
                .switchToCurrentThread()
                .nextDo(ret -> {
                    if (ret) {
                        ToClientResp success = ToClientResp.newBuilder()
                                .setErrorCode(ToClientResp.ErrorType.OK_VALUE)
                                .setErrorMsg("ok")
                                .build();
                        return Homo.result(success);
                    } else {
                        ToClientResp error = ToClientResp.newBuilder()
                                .setErrorCode(ToClientResp.ErrorType.USER_NOT_FOUND_VALUE)
                                .setErrorMsg("not found")
                                .build();
                        return Homo.result(error);
                    }
                });
    }

    @Override
    public Homo<ToClientResp> sendToAllClient(Integer podId, ToClientReq req) {
        return Homo.warp(()->{
            Collection<ProxyGateClient> clients = ProxyGateClientMgr.uidToGateClientMap.values();
            for (ProxyGateClient client : clients) {
                try {
                    client.toClient(podId,req).start();
                }catch (Exception e){
                    log.error("sendToAllClient to userId {} error. msgId {}", client.getUid(), req, e);
                }
            }
            return Homo.result(ToClientResp.newBuilder().build());
        });
    }

    //rpc调用, 加入全局队列
    //本地请直接调用 removeLocalClient方法
    @Override
    public Homo<Boolean> kickLocalClient(Integer podId, String uid) {
        log.info("kickLocalClient uid_{}", uid);
        ProxyGateClient gateClient = ProxyGateClientMgr.getFromValid(uid);
        if(gateClient == null){
            log.error("removeLocalClient [{}] failed! already removed", uid);
            return Homo.result(true);
        }
        return gateClient.kickPromise(true);
    }

    @Override
    public Homo<TransferCacheResp> transferCache(Integer podId, TransferCacheReq req) {
        String userId = req.getUserId();
        int fromPodId = req.getPodId();
        SyncInfo syncInfo = req.getSyncInfo();
        log.info("transferCache userId {} from podId {}", userId, fromPodId);
        ProxyGateClient gateClient = ProxyGateClientMgr.getFromValid(userId);
        if (gateClient == null) {
            log.error("transferCache failed uid {} not exist or valid", userId);
            return Homo.result(TransferCacheResp.newBuilder().setErrorCode(TransferCacheResp.ErrorType.USER_NOT_FOUND).build());
        }
        TransferCacheResp transferCacheResp = gateClient.transfer(userId, syncInfo, fromPodId);
        if (transferCacheResp.getErrorCodeValue() == HomoCommonError.success.getCode()) {
            //重连成功,发送断线通知
            DisconnectMsg disconnectMsg = DisconnectMsg.newBuilder().build();
            return gateClient.sendToClientComplete(disconnectMsg.toByteArray())
                    .switchToCurrentThread()
                    .nextDo(ret -> {
                        //待验证，之前是removeFromTransferredIfPresent
                        ProxyGateClientMgr.removeFromValid(userId);
                        return Homo.result(transferCacheResp);
                    });
        }
        return Homo.result(TransferCacheResp.newBuilder().setErrorCode(TransferCacheResp.ErrorType.ERROR).build());
    }

    @Override
    public Homo<ToClientResp> forwardMsg(Integer podId, ToClientReq req) {
        String userId = req.getClientId();
        ProxyGateClient gateClient = ProxyGateClientMgr.getFromValid(userId);
        if(gateClient == null){
            ToClientResp clientResp = ToClientResp.newBuilder()
                    .setErrorCode(HomoCommonError.gate_client_not_found.getCode())
                    .setErrorMsg(HomoCommonError.gate_client_not_found.message())
                    .build();
            return Homo.result(clientResp);
        }
        return gateClient.toClient(podId,req)
                .nextDo(ret->{
                    ToClientResp clientResp;
                    if (ret){
                        clientResp = ToClientResp.newBuilder()
                                .setErrorCode(HomoCommonError.success.getCode())
                                .setErrorMsg(HomoCommonError.success.message())
                                .build();
                    }else {
                        clientResp = ToClientResp.newBuilder()
                                .setErrorCode(HomoCommonError.gate_client_transfer_fail.getCode())
                                .setErrorMsg(HomoCommonError.gate_client_transfer_fail.message())
                                .build();
                    }
                    return Homo.result(clientResp);
                });
    }
}
