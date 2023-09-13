package com.homo.game.stateful.proxy.gate;

import com.core.ability.base.StorageEntityMgr;
import com.homo.core.facade.gate.GateClient;
import com.homo.core.facade.gate.GateMessage;
import com.homo.core.facade.lock.LockDriver;
import com.homo.core.facade.service.ServiceStateMgr;
import com.homo.core.gate.tcp.handler.ProtoLogicHandler;
import com.homo.core.rpc.base.service.ServiceMgr;
import com.homo.core.rpc.client.RpcClientMgr;
import com.homo.core.rpc.client.proxy.RpcProxyMgr;
import com.homo.core.utils.concurrent.queue.IdCallQueue;
import com.homo.core.utils.exception.HomoError;
import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.spring.GetBeanUtil;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.game.proxy.handler.RouterHandler;
import com.homo.game.proxy.handler.RouterHandlerManger;
import com.homo.game.stateful.proxy.StatefulProxyService;
import io.homo.proto.client.*;
import io.homo.proto.entity.EntityRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
@Slf4j
public class LoginPbLogicHandler extends ProtoLogicHandler {
    @Autowired
    RouterHandlerManger routerHandlerMgr;
    @Autowired
    ProxyGateClientMgr proxyGateClientMgr;
    IdCallQueue idCallQueue = new IdCallQueue("loginQueue", 1000 * 11, IdCallQueue.DropStrategy.DROP_CURRENT_TASK);
    @Autowired
    ServiceStateMgr serviceStateMgr;
    @Autowired
    StatefulProxyService localProxyService;
    @Autowired
    RpcClientMgr rpcClientMgr;
    @Autowired
    RpcProxyMgr rpcProxyMgr;
    @Autowired
    LockDriver lockDriver;
    @Autowired
    StorageEntityMgr entityMgr;

    @Override
    public void process(Msg msg, GateClient gateClient, GateMessage.Header header) throws Exception {
        String msgId = msg.getMsgId();
        short sessionId = header.getSessionId();
        short sendSeq = header.getSendSeq();
        short recvSeq = header.getRecvSeq();
        ProxyGateClient proxyGateClient = (ProxyGateClient) gateClient;
        // 更新重连信息
        proxyGateClient.updateReconnectInfo(sendSeq, recvSeq, sessionId);
        if (msgId.equals(LoginMsgReq.class.getSimpleName())) {
            LoginMsgReq loginMsgReq = LoginMsgReq.parseFrom(msg.getMsgContent());
            processLogin(loginMsgReq, null, proxyGateClient, header);
        } else if (msgId.equals(LoginAndSyncReq.class.getSimpleName())) {
            LoginAndSyncReq loginAndSyncReq = LoginAndSyncReq.parseFrom(msg.getMsgContent());
            LoginMsgReq loginMsgReq = loginAndSyncReq.getLoginMsgReq();
            processLogin(loginMsgReq, loginAndSyncReq.getSyncInfo(), proxyGateClient, header);
        } else if (msgId.equals(ClientRouterMsg.class.getSimpleName())) {
            processRouterMsg(msgId, msg, sessionId, proxyGateClient);
        }
    }
    private void processRouterMsg(String msgId, Msg msg, short sessionId, ProxyGateClient proxyGateClient) {
        //todo tpc proxy转发逻辑实现
    }

    private void processEntityRouterMsg() {

    }

    public void processLogin(LoginMsgReq loginMsgReq, SyncInfo syncInfo, ProxyGateClient gateClient, GateMessage.Header header) {
        String userId = loginMsgReq.getUserId();
        String channelId = loginMsgReq.getChannelId();
        String token = loginMsgReq.getToken();
        String appVersion = loginMsgReq.getAppVersion();
        String resVersion = loginMsgReq.getResVersion();
        String adId = loginMsgReq.getAdId();
        Homo.queue(idCallQueue, new Callable<Homo<LoginMsgResp>>() {
                    @Override
                    public Homo<LoginMsgResp> call() throws Exception {
                        return Homo.warp(sink -> {
                            routerHandlerMgr.create(sink, "checkParamMsgHandler", "checkWhiteListHandler", "checkUserNumberHandler",
                                            "authTokenHandler")
                                    .setParam(RouterHandler.PARAM_USER_ID, userId)
                                    .setParam(RouterHandler.PARAM_CHANNEL_ID, channelId)
                                    .setParam(RouterHandler.PARAM_TOKEN, token)
                                    .sort()
                                    .process();
                        });
                    }
                }, null)
                .consumerValue(resp -> {
                    if (resp.getErrorCodeValue() == HomoCommonError.success.getCode()) {
                        //如果登陆认证成功，登陆后处理
                        afterAuthLoginProcess(userId, gateClient, syncInfo)
                                .consumerValue(loginMsgResp -> {
                                    Msg.Builder gateMsgResp = Msg.newBuilder();
                                    Msg msg = gateMsgResp.setMsgContent(loginMsgResp.toByteString()).build();
                                    gateClient.sendToClient(msg.toByteArray());
                                });
                    } else {
                        Msg.Builder gateMsgResp = Msg.newBuilder();
                        LoginMsgResp loginMsgResp = LoginMsgResp.newBuilder()
                                .setErrorCode(resp.getErrorCode())
                                .setErrorMsg(resp.getErrorMsg())
                                .build();

                        Msg msg = gateMsgResp.setMsgContent(loginMsgResp.toByteString()).build();
                        gateClient.sendToClient(msg.toByteArray());
                    }
                })
                .start();
    }

    /**
     * 登陆校验成功后处理
     * 1 新登陆
     * 检查是否有旧的登陆，如果有旧的登陆，发送断线通知
     * 踢掉旧连接的gateClient，关闭channel
     * 2 断线重连
     * 从旧连接取出缓存的消息，发送给新连接
     *
     * @param userId
     * @param proxyGateClient
     * @param syncInfo
     * @return
     */
    private Homo<LoginMsgResp> afterAuthLoginProcess(String userId, ProxyGateClient proxyGateClient, SyncInfo syncInfo) {
        Homo<Boolean> processPromise;
        if (syncInfo != null) {
            processPromise = reconnectProcess(userId, proxyGateClient, syncInfo)
                    .consumerValue(processOk -> {
                        proxyGateClient.setState(ProxyGateClient.State.RECONNECTED);
                    });
        } else {
            processPromise = newLoginProcess(userId, proxyGateClient)
                    .consumerValue(processOk -> {
                        proxyGateClient.setState(ProxyGateClient.State.LOGIN);
                    });
        }
        return processPromise
                .nextDo(processOk -> {
                    if (processOk) {
                        LoginMsgResp LOGIN_SUCCESS = LoginMsgResp.newBuilder().setErrorMsg("success").setErrorCode(LoginMsgResp.ErrType.OK).build();
                        return Homo.result(LOGIN_SUCCESS);
                    } else {
                        LoginMsgResp LOGIN_FAIL = LoginMsgResp.newBuilder().setErrorMsg("fail").setErrorCode(LoginMsgResp.ErrType.OTHER_LOGIN).build();
                        return Homo.result(LOGIN_FAIL);
                    }
                });
    }

    private Homo<Boolean> reconnectProcess(String userId, ProxyGateClient proxyGateClient, SyncInfo syncInfo) {
        Integer podIndex = serviceStateMgr.getPodIndex();
        //reconnect
        return serviceStateMgr.getLinkedPod(userId, localProxyService.getTagName())
                .nextDo(linkPodId -> {
                    TransferCacheReq cacheReq = TransferCacheReq.newBuilder().setUserId(userId).setSyncInfo(syncInfo).setPodId(podIndex).build();
                    StatefulProxyService proxyService = getProxyService(linkPodId);
                    //获取先前连接的缓存包
                    return proxyService.transferCache(linkPodId, cacheReq)
                            .nextDo(transferCacheResp -> {
                                if (transferCacheResp.getErrorCodeValue() != HomoCommonError.success.getCode()) {
                                    log.error("processReconnect userId {} transferCacheResp {} ", userId, transferCacheResp);
                                    return Homo.error(HomoError.throwError(HomoCommonError.common_system_error));
                                }
                                proxyGateClient.recvTransferMsgs(transferCacheResp);
                                ProxyGateClientMgr.putToValid(userId, proxyGateClient);
                                return Homo.result(true);
                            });
                });
    }

//    private Homo<ClientSendHandler> createClientSendHandler(String userId, ProxyGateClient proxyGateClient, ServerInfo serverInfo) {
//        final String lockField = "lock_" + userId;
//        final String uuid = UUID.randomUUID().toString();
//        //获取登录锁
//        return lockDriver.asyncLock(serverInfo.getAppId(), serverInfo.getRegionId(), "1", lockField, uuid, 3)
//                .nextDo(lockSuccess -> {
//                    if (!lockSuccess) {
//                        //加锁失败,说明有人正在登录,返回失败
//                        Homo.error(HomoError.throwError(HomoCommonError.common_system_error));
//                    }
//                    return entityMgr.createEntityPromise(ClientSendHandler.class, userId);
//                })
//                .consumerValue(ret -> {
//                    lockDriver.asyncUnlock(serverInfo.getAppId(), serverInfo.getRegionId(), "1", lockField, uuid).start();
//                })
//                .catchError(throwable -> {
//                    log.error("createClientSendHandler error userId {} throwable", userId, throwable);
//                    lockDriver.asyncUnlock(serverInfo.getAppId(), serverInfo.getRegionId(), "1", lockField, uuid).start();
//                })
//                ;
//    }

    public Homo<Boolean> newLoginProcess(String userId, ProxyGateClient proxyGateClient) {

        return serviceStateMgr.getUserLinkedPodNoCache(userId, localProxyService.getHostName())
                .nextDo(oldPod -> {
                            if (oldPod != null) {
                                //如果已经登陆，先通知之前的客户端断开信息，再踢掉之前的登陆
                                DisconnectMsg disconnectMsg = DisconnectMsg.newBuilder()
                                        .setErrorCode(DisconnectMsg.ErrorType.OTHER_USER_LOGIN)
                                        .setErrorMsg("other user login")
                                        .build();
                                ToClientReq disReq = ToClientReq.newBuilder()
                                        .setClientId(userId)
                                        .setMsgType(DisconnectMsg.class.getSimpleName())
                                        .setMsgContent(disconnectMsg.toByteString())
                                        .build();
                                //之前登陆的是其他pod,给其他pod发送踢人消息
                                StatefulProxyService remoteProxyService = getProxyService(oldPod);
                                //先断开连接
                                return remoteProxyService.sendToClientComplete(oldPod, disReq)
                                        .nextDo(resp -> {
                                            if (resp.getErrorCode() != HomoCommonError.success.getCode()) {
                                                log.error("disconnectMsg old login error userId {} oldPod {}", userId, oldPod);
                                            }
                                            //再踢人
                                            return remoteProxyService.kickLocalClient(oldPod, userId);
                                        });
                            } else {
                                return Homo.result(true);
                            }
                        }
                )
                .onErrorContinue(throwable -> {
                    log.error("checkAndKickOldLogin error userId {} throwable", userId, throwable);
                    return Homo.result(false);
                })
                ;

    }

    @Getter
    static StatefulProxyService remoteProxyService;

    public StatefulProxyService getProxyService(Integer podId) {
        if (localProxyService.getPodIndex().equals(podId)) {
            return localProxyService;
        }
        if (remoteProxyService == null) {
            RpcClientMgr rpcClient = GetBeanUtil.getBean(RpcClientMgr.class);
            ServiceStateMgr serviceStateMgr = GetBeanUtil.getBean(ServiceStateMgr.class);
            ServiceMgr serviceMgr = GetBeanUtil.getBean(ServiceMgr.class);
            try {
                remoteProxyService = RpcProxyMgr.createProxy(rpcClient, StatefulProxyService.class, serviceMgr, serviceStateMgr);
            } catch (Exception e) {
                log.error("getProxy error ", e);
            }
        }
        return remoteProxyService;
    }

}
