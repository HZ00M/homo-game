package com.homo.game.stateful.proxy;

import com.homo.core.rpc.base.service.BaseService;
import com.homo.core.utils.concurrent.event.Event;
import com.homo.core.utils.concurrent.queue.CallQueueMgr;
import com.homo.core.utils.concurrent.schedule.HomoTimerMgr;
import com.homo.core.utils.exception.HomoError;
import com.homo.core.utils.rector.Homo;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.game.stateful.proxy.config.StatefulProxyProperties;
import com.homo.game.stateful.proxy.facade.IStatefulProxyService;
import com.homo.game.stateful.proxy.gate.ProxyGateClient;
import com.homo.game.stateful.proxy.gate.ProxyGateClientMgr;
import io.homo.proto.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.function.BiFunction;

@Component
@Slf4j
public class StatefulProxyService extends BaseService implements IStatefulProxyService {
    @Autowired
    private ProxyGateClientMgr gateClientMgr;
    @Autowired
    private StatefulProxyProperties statefulProxyProperties;
    @Override
    public void postInit(){
//        CallQueueMgr.registerPloy("toClient", new BiFunction<Event, Object, Integer>() {
//            @Override
//            public Integer apply(Event event, Object o) {
//                return null;
//            }
//        });
    }
    @Override
    public Homo<ToClientResp> sendToClient(Integer podId, ParameterMsg parameterMsg, ToClientReq req) {
        String userId = req.getClientId();
        log.info("sendToClient call userId {} msgType {}", userId, req.getMsgType());
        ProxyGateClient gateClient = ProxyGateClientMgr.getClientByUid(userId);
        if (gateClient == null) {
            log.error("sendToClient error gateClient == null userId {}", userId);
            ToClientResp success = ToClientResp.newBuilder()
                    .setErrorCode(HomoCommonError.send_to_client_gate_not_found.getCode())
                    .setErrorMsg(HomoCommonError.send_to_client_gate_not_found.message())
                    .build();
            return Homo.result(success);
        }
        //缓存服务器发往客户端的消息，发送失败时支持重发，支持断线重连
        gateClient.cacheMsg(req.getMsgType(), req.getMsgContent().toByteArray());
        return gateClient.sendToClient(req.getMsgType(), req.getMsgContent().toByteArray())
                .nextDo(ret -> {
                    if (ret) {
                        ToClientResp success = ToClientResp.newBuilder()
                                .setErrorCode(HomoError.success.getCode())
                                .setErrorMsg(HomoError.success.message())
                                .build();
                        return Homo.result(success);
                    } else {
                        ToClientResp error = ToClientResp.newBuilder()
                                .setErrorCode(HomoCommonError.send_to_client_fail.getCode())
                                .setErrorMsg(HomoCommonError.send_to_client_fail.message())
                                .build();
                        return Homo.result(error);
                    }
                });
    }

    @Override
    public Homo<ToClientResp> sendToClientComplete(Integer podId, ParameterMsg parameterMsg, ToClientReq req) {
        String userId = req.getClientId();
        ProxyGateClient gateClient = ProxyGateClientMgr.getClientByUid(userId);
        if (gateClient == null) {
            log.info("sendToClientComplete gateClient == null podId {} userId {}", podId, userId);
            ToClientResp success = ToClientResp.newBuilder()
                    .setErrorCode(HomoCommonError.send_to_client_gate_not_found.getCode())
                    .setErrorMsg(HomoCommonError.send_to_client_gate_not_found.message())
                    .build();
            return Homo.result(success);
        }
        gateClient.cacheMsg(req.getMsgType(), req.getMsgContent().toByteArray());
        return gateClient.sendToClientComplete(req.getMsgType(), req.getMsgContent().toByteArray())
                .switchToCurrentThread()
                .nextDo(ret -> {
                    if (ret) {
                        ToClientResp success = ToClientResp.newBuilder()
                                .setErrorCode(HomoError.success.getCode())
                                .setErrorMsg(HomoError.success.message())
                                .build();
                        return Homo.result(success);
                    } else {
                        ToClientResp error = ToClientResp.newBuilder()
                                .setErrorCode(HomoCommonError.send_to_client_fail.getCode())
                                .setErrorMsg(HomoCommonError.send_to_client_fail.message())
                                .build();
                        return Homo.result(error);
                    }
                });
    }

    @Override
    public Homo<ToClientResp> sendToAllClient(Integer podId, ToClientReq req) {
        return Homo.warp(() -> {
            Collection<ProxyGateClient> clients = ProxyGateClientMgr.clientNameToGateClientMap.values();
            for (ProxyGateClient client : clients) {
                try {
                    client.toClient(podId, req).start();
                } catch (Exception e) {
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
        ProxyGateClient gateClient = ProxyGateClientMgr.getClientByUid(uid);
        if (gateClient == null) {
            log.warn("kickLocalClient already removed  uid {} ", uid);
            return Homo.result(true);
        }
        return gateClient.kickPromise(true)
                .consumerValue(ret -> {
                    log.info("kickPromise success {} uid {} clientName {} ", ret, uid, gateClient.name());
                });
    }

    @Override
    public Homo<TransferCacheResp> transferCache(Integer podId, TransferCacheReq req) {
        String userId = req.getUserId();
        int fromPodId = req.getPodId();
        SyncInfo syncInfo = req.getSyncInfo();
        log.info("transferCache userId {} from podId {}", userId, fromPodId);
        ProxyGateClient gateClient = ProxyGateClientMgr.getClientByUid(userId);
        if (gateClient == null) {
            log.error("transferCache failed uid {} not exist or valid", userId);
            return Homo.result(TransferCacheResp.newBuilder().setErrorCode(TransferCacheResp.ErrorType.USER_NOT_FOUND).build());
        }
        TransferCacheResp transferCacheResp = gateClient.transfer(userId, syncInfo, fromPodId);
        log.info("transferCache transfer userId {} innerSendSeq {} clientSendSeq {} transferCacheResp.size {}",
                userId, transferCacheResp.getSendSeq(), transferCacheResp.getRecvSeq(), transferCacheResp.getSyncMsgListList().size());
        if (transferCacheResp.getErrorCodeValue() == HomoError.success.getCode()) {
            //重连成功,发送断线通知,尽力通知
            DisconnectMsg disconnectMsg = DisconnectMsg.newBuilder().build();
            return gateClient.sendToClient(DisconnectMsg.class.getSimpleName(), disconnectMsg.toByteArray())
                    .nextDo(ret -> {
                        //将用户在本pod的消息缓存转移后将gateClient对象移除，这里
                        HomoTimerMgr.getInstance().once("transferCache_" + userId, CallQueueMgr.getInstance().getQueueByUid(userId), () -> {
                            ProxyGateClientMgr.unBindGate(userId, gateClient);
                            log.info("transferCache finish removeFromValid gateClient userId {} sendToClient disconnectMsg {}", userId, ret);
                        }, statefulProxyProperties.getTransferRemoveDelayMillisecond());
                        log.info("transferCache finish sendToClient userId {} ret {}", userId, ret);
                        return Homo.result(transferCacheResp);
                    });
        }
        return Homo.result(TransferCacheResp.newBuilder().setErrorCode(TransferCacheResp.ErrorType.ERROR).build());
    }

    @Override
    public Homo<ToClientResp> forwardMsg(Integer podId, ToClientReq req) {
        String userId = req.getClientId();
        ProxyGateClient gateClient = ProxyGateClientMgr.getClientByUid(userId);
        if (gateClient == null) {
            ToClientResp clientResp = ToClientResp.newBuilder()
                    .setErrorCode(HomoCommonError.gate_client_not_found.getCode())
                    .setErrorMsg(HomoCommonError.gate_client_not_found.message())
                    .build();
            return Homo.result(clientResp);
        }
        return gateClient.toClient(podId, req)
                .nextDo(ret -> {
                    ToClientResp clientResp;
                    if (ret) {
                        clientResp = ToClientResp.newBuilder()
                                .setErrorCode(HomoError.success.getCode())
                                .setErrorMsg(HomoError.success.message())
                                .build();
                    } else {
                        clientResp = ToClientResp.newBuilder()
                                .setErrorCode(HomoCommonError.gate_client_transfer_fail.getCode())
                                .setErrorMsg(HomoCommonError.gate_client_transfer_fail.message())
                                .build();
                    }
                    return Homo.result(clientResp);
                });
    }
}
