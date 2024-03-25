package com.homo.game.stateful.proxy.gate;

import com.core.ability.base.StorageEntityMgr;
import com.google.protobuf.ByteString;
import com.homo.core.facade.gate.GateClient;
import com.homo.core.facade.gate.GateMessageHeader;
import com.homo.core.facade.lock.LockDriver;
import com.homo.core.facade.service.ServiceStateMgr;
import com.homo.core.gate.tcp.handler.ProtoGateLogicHandler;
import com.homo.core.rpc.base.serial.ByteRpcContent;
import com.homo.core.rpc.base.service.ServiceMgr;
import com.homo.core.rpc.client.RpcClientMgr;
import com.homo.core.rpc.client.proxy.RpcProxyMgr;
import com.homo.core.utils.concurrent.event.AbstractTraceEvent;
import com.homo.core.utils.concurrent.event.Event;
import com.homo.core.utils.concurrent.queue.CallQueueMgr;
import com.homo.core.utils.concurrent.queue.IdCallQueue;
import com.homo.core.utils.exception.HomoError;
import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.spring.GetBeanUtil;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.game.proxy.handler.RouterHandler;
import com.homo.game.proxy.handler.RouterHandlerManger;
import com.homo.game.stateful.proxy.StatefulProxyService;
import io.homo.proto.client.*;
import io.netty.util.internal.StringUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;

import java.util.concurrent.Callable;

@Component
@Slf4j
public class StatefulProxyLogicHandler extends ProtoGateLogicHandler {
    @Autowired
    RouterHandlerManger routerHandlerMgr;
    @Autowired
    ProxyGateClientMgr proxyGateClientMgr;
    IdCallQueue idCallQueue = new IdCallQueue("loginQueue", 1000 * 11, IdCallQueue.DropStrategy.DROP_CURRENT_TASK, 3);
    @Autowired
    ServiceStateMgr serviceStateMgr;
    @Autowired
    StatefulProxyService localProxyService;
    @Autowired
    RpcClientMgr rpcClientMgr;
    @Autowired
    LockDriver lockDriver;
    @Autowired
    StorageEntityMgr entityMgr;

    @Override
    public void doProcess(Msg msg, GateClient gateClient, GateMessageHeader header) throws Exception {
        ProxyGateClient proxyGateClient = (ProxyGateClient) gateClient;
        if (msg.getMsgId().equals(LoginMsgReq.class.getSimpleName()) || msg.getMsgId().equals(LoginAndSyncReq.class.getSimpleName())) {
            CallQueueMgr.getInstance().addEvent(CallQueueMgr.frame_queue_id, new AbstractTraceEvent() {
                @Override
                public void process() {
                    //切换到业务线程
                    try {
                        handlerTcpCall(msg, gateClient, header);
                    } catch (Exception e) {
                        log.error("handlerTcpCall error gateClient {}", gateClient, e);
                    }
                }
            });
        } else {
            if (proxyGateClient.uid == null) {
                log.warn("no login yet gateClient {} msgId {}", gateClient.name(), msg.getMsgId());
                return;
            }
            CallQueueMgr.getInstance().addEvent(CallQueueMgr.getInstance().choiceQueueIdBySeed(proxyGateClient.getUid().hashCode()), new AbstractTraceEvent() {
                @Override
                public void process() {
                    //切换到业务线程
                    try {
                        handlerTcpCall(msg, gateClient, header);
                    } catch (Exception e) {
                        log.error("handlerTcpCall error gateClient {}", gateClient, e);
                    }
                }
            });
        }

    }

    void handlerTcpCall(Msg msg, GateClient gateClient, GateMessageHeader header) throws Exception {
        String msgId = msg.getMsgId();
        short sessionId = header.getSessionId();
        short sendSeq = header.getSendSeq();
        short recvSeq = header.getRecvSeq();
        log.info("LoginPbLogicHandler doProcess msgId {} sessionId {} clientSendSeq {} confirmServerSendSeq {}", msgId, sessionId, sendSeq, recvSeq);
        ProxyGateClient proxyGateClient = (ProxyGateClient) gateClient;
        // 更新重连信息
        if (header.getBodySize() == 0) {
            msg = null; //请求包没有body 说明是心跳包
        }
        proxyGateClient.syncMsgSeq(sendSeq, recvSeq, sessionId);
        if (msgId.equals(LoginMsgReq.class.getSimpleName())) {
            LoginMsgReq loginMsgReq = LoginMsgReq.parseFrom(msg.getMsgContent());
            processLogin(loginMsgReq, null, proxyGateClient, header);
        } else if (msgId.equals(LoginAndSyncReq.class.getSimpleName())) {
            LoginAndSyncReq loginAndSyncReq = LoginAndSyncReq.parseFrom(msg.getMsgContent());
            LoginMsgReq loginMsgReq = loginAndSyncReq.getLoginMsgReq();
            processLogin(loginMsgReq, loginAndSyncReq.getSyncInfo(), proxyGateClient, header);
        } else if (msgId.equals(TransferCacheReq.class.getSimpleName())) {
            TransferCacheReq transferCacheReq = TransferCacheReq.parseFrom(msg.getMsgContent());
            processTransferMsg(proxyGateClient.getUid(), proxyGateClient, transferCacheReq);
        } else if (msgId.equals(ClientRouterMsg.class.getSimpleName())) {
            ClientRouterMsg clientRouterMsg = ClientRouterMsg.parseFrom(msg.getMsgContent());
            processRouterMsg(clientRouterMsg, proxyGateClient, header);
        }
    }

    private void processRouterMsg(ClientRouterMsg clientRouterMsg, ProxyGateClient gateClient, GateMessageHeader header) throws Exception {
        if (!gateClient.isLogin()) {
            log.error("uid [{}] send message without login", clientRouterMsg.getUserId());
            return;
        }
        CallQueueMgr callQueueMgr = CallQueueMgr.getInstance();
        Event event = () -> {
            Homo.warp(homoSink -> {
                String tag = clientRouterMsg.getSrcService();
                if (!StringUtil.isNullOrEmpty(clientRouterMsg.getEntityType())) {
                    tag = clientRouterMsg.getEntityType();
                }
                serviceStateMgr.getServiceInfo(tag)
                        .consumerValue(serviceInfo -> {
                            ParameterMsg parameterMsg = ParameterMsg.newBuilder()
                                    .setUserId(clientRouterMsg.getUserId())
                                    .setChannelId(clientRouterMsg.getChannelId())
                                    .build();
                            routerHandlerMgr.create(homoSink, "defaultRouterHandler")
                                    .router(clientRouterMsg)
                                    .setParam(RouterHandler.PARAM_SRC_SERVICE, serviceInfo.getServiceTag())
                                    .setParam(RouterHandler.PARAMETER_MSG, parameterMsg)
                                    .process();
                        });
            }).consumerValue(ret -> {
                Tuple2<String, ByteRpcContent> tuple = (Tuple2<String, ByteRpcContent>) ret;
                String msgId = tuple.getT1();
                byte[][] data = tuple.getT2().getData();
                Msg.Builder gateMsgResp = Msg.newBuilder();
                Msg msg = gateMsgResp.setMsgId(tuple.getT1()).setMsgContent(ByteString.copyFrom(data[0])).build();
                gateClient.sendToClient(msgId, msg.toByteArray());
                log.info("processRouterMsg success userId {} msgId {}", gateClient.getUid(), msgId);
            });
        };
        callQueueMgr.addEvent(callQueueMgr.choiceQueueIdBySeed(gateClient.getQueueId()), event);//lambda表达式无法进出断点，移到上面
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
                    .nextDo(processOk -> {
                        if (processOk) {
                            proxyGateClient.reconnect(userId);
                        } else {
                            log.warn("reconnectProcess processOk false userId {}", userId);
                        }
                        return Homo.result(processOk);
                    });
        } else {
            processPromise = newLoginProcess(userId, proxyGateClient)
                    .nextDo(processOk -> {
                        if (processOk) {
                            proxyGateClient.newLogin(userId);
                        } else {
                            log.warn("newLoginProcess processOk false userId {}", userId);
                        }
                        return Homo.result(processOk);
                    });
        }
        return processPromise
                .nextDo(processOk -> {
                    if (processOk) {
                        log.info("login finish userId {} ret {}", userId, processOk);
                        LoginMsgResp LOGIN_SUCCESS = LoginMsgResp.newBuilder().setErrorMsg(HomoError.success.message()).setErrorCode(HomoError.success.getCode()).build();
                        return Homo.result(LOGIN_SUCCESS);
                    } else {
                        LoginMsgResp LOGIN_FAIL = LoginMsgResp.newBuilder().setErrorMsg(HomoCommonError.common_system_error.message()).setErrorCode(HomoCommonError.common_system_error.getCode()).build();
                        return Homo.result(LOGIN_FAIL);
                    }
                });
    }

    public void processLogin(LoginMsgReq loginMsgReq, SyncInfo syncInfo, ProxyGateClient gateClient, GateMessageHeader header) {
        String userId = loginMsgReq.getUserId();
        String channelId = loginMsgReq.getChannelId();
        String token = loginMsgReq.getToken();
        String appVersion = loginMsgReq.getAppVersion();
        String resVersion = loginMsgReq.getResVersion();
        String adId = loginMsgReq.getAdId();
        Homo
//                .result(true)
//                .switchThread(0)
                .queue(idCallQueue, new Callable<Homo<LoginMsgResp>>() {
                    @Override
                    public Homo<LoginMsgResp> call() throws Exception {
                        log.info("processLogin start userId {}", userId);
                        return Homo.warp(sink -> {
                            Homo.warp(checkSink -> {
                                        routerHandlerMgr.create(checkSink, "checkParamMsgHandler", "checkWhiteListHandler", "checkUserNumberHandler",
                                                        "authTokenHandler")
                                                .setParam(RouterHandler.PARAM_USER_ID, userId)
                                                .setParam(RouterHandler.PARAM_MSG_ID, LoginMsgReq.class.getSimpleName())
                                                .setParam(RouterHandler.PARAM_CHANNEL_ID, channelId)
                                                .setParam(RouterHandler.PARAM_TOKEN, token)
                                                .process()
                                        ;
                                    })
                                    .switchToCurrentThread()
                                    .consumerValue(res -> {
                                        Tuple2<Boolean, String> tuple = (Tuple2<Boolean, String>) res;
                                        if (tuple.getT1()) {
                                            //如果登陆认证成功，登陆后处理
                                            afterAuthLoginProcess(userId, gateClient, syncInfo)
                                                    .consumerValue(loginMsgResp -> {
                                                        Msg.Builder gateMsgResp = Msg.newBuilder();
                                                        String msgId = LoginMsgResp.class.getSimpleName();
                                                        Msg msg = gateMsgResp.setMsgId(msgId).setMsgContent(loginMsgResp.toByteString()).build();
                                                        gateClient.sendToClient(msgId, msg.toByteArray());
                                                        log.info("processLogin success userId {}", userId);
                                                        sink.success(loginMsgResp);
                                                    })
                                                    .catchError(throwable -> {
                                                        log.error("processLogin error userId {}", userId, throwable);
                                                    })
                                                    .start();
                                        } else {
                                            Msg.Builder gateMsgResp = Msg.newBuilder();
                                            LoginMsgResp loginMsgResp = LoginMsgResp.newBuilder()
                                                    .setErrorCode(HomoCommonError.logon_fail.getCode())
                                                    .setErrorMsg(HomoCommonError.logon_fail.message())
                                                    .build();
                                            String msgId = LoginMsgResp.class.getSimpleName();
                                            Msg msg = gateMsgResp.setMsgId(msgId).setMsgContent(loginMsgResp.toByteString()).build();
                                            gateClient.sendToClient(msgId, msg.toByteArray());
                                            log.info("processLogin fail userId {}", userId);
                                            sink.success(loginMsgResp);
                                        }
                                    });
                        });
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        log.error("processLogin timeout userId {}", userId);
                    }
                }).start();
    }

    public void processTransferMsg(String userId, ProxyGateClient proxyGateClient, TransferCacheReq req) {
        Integer podIndex = localProxyService.getPodIndex();
        localProxyService.transferCache(podIndex, req)
                .nextDo(transferCacheResp -> {
                    if (transferCacheResp.getErrorCodeValue() != HomoError.success.getCode()) {
                        log.error("transferMsgProcess error userId {} transferCacheResp {} ", userId, transferCacheResp);
                        return Homo.result(false);
                    }
                    log.info("transferMsgProcess success userId {} sendSeq {} recvSeq {} size {}",
                            userId, transferCacheResp.getSendSeq(), transferCacheResp.getRecvSeq(), transferCacheResp.getSyncMsgListCount());
                    proxyGateClient.recvTransferMsgs(transferCacheResp);
                    proxyGateClient.sendToClient("TransferCacheResp", transferCacheResp.toByteArray());
                    return Homo.result(true);
                }).start();
    }

    private Homo<Boolean> reconnectProcess(String userId, ProxyGateClient proxyGateClient, SyncInfo syncInfo) {
        Integer podIndex = serviceStateMgr.getPodIndex();
        //reconnect
        return serviceStateMgr.getLinkedPod(userId, localProxyService.getHostName())
                .nextDo(linkPodId -> {
                    TransferCacheReq cacheReq = TransferCacheReq.newBuilder().setUserId(userId).setSyncInfo(syncInfo).setPodId(podIndex).build();
                    StatefulProxyService proxyService = getProxyService(linkPodId);
                    //获取先前连接的缓存包
                    if (syncInfo.getCount() > 0) {
                        return proxyService.transferCache(linkPodId, cacheReq)
                                .nextDo(transferCacheResp -> {
                                    if (transferCacheResp.getErrorCodeValue() != HomoError.success.getCode()) {
                                        log.error("processReconnect userId {} transferCacheResp {} ", userId, transferCacheResp);
                                        return Homo.error(HomoError.throwError(HomoCommonError.common_system_error));
                                    }
                                    proxyGateClient.recvTransferMsgs(transferCacheResp);
//                                ProxyGateClientMgr.putToValid(userId, proxyGateClient);
                                    return Homo.result(true);
                                });
                    } else {
                        return Homo.result(true);
                    }

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
//                .switchThread(CallQueueMgr.getInstance().getQueueByUid(userId).getId(), null)
                .nextDo(oldPod -> {
                            log.info("newLoginProcess start userId {} getUserLinkedPodNoCache stateful-proxy oldPod {}", userId, oldPod);
                            if (oldPod != null && oldPod != -1) {
                                //如果已经登陆，先通知之前的客户端断开信息，再踢掉之前的登陆
                                DisconnectMsg disconnectMsg = DisconnectMsg.newBuilder()
                                        .setErrorCode(DisconnectMsg.ErrorType.OTHER_USER_LOGIN)
                                        .setErrorMsg("other user login")
                                        .build();
                                ToClientReq disconnectNotify = ToClientReq.newBuilder()
                                        .setClientId(userId)
                                        .setMsgType(DisconnectMsg.class.getSimpleName())
                                        .setMsgContent(disconnectMsg.toByteString())
                                        .build();
                                //之前登陆的是其他pod,给其他pod发送踢人消息
                                StatefulProxyService beforeConnectProxyService = getProxyService(oldPod);
                                ParameterMsg parameterMsg = ParameterMsg.newBuilder().setUserId(userId).build();
                                //先断开连接,有可能已经断开了，所以不需要等待结果，使用sendToClient
                                return beforeConnectProxyService.sendToClient(oldPod, parameterMsg, disconnectNotify)
                                        .nextDo(resp -> {
                                            log.info("newLoginProcess disconnectMsg sendToClientComplete old login  userId {} oldPod {} resp {}", userId, oldPod, resp.getErrorCode());
                                            //再踢人
                                            return beforeConnectProxyService.kickLocalClient(oldPod, userId)
                                                    .consumerValue(ret -> {
                                                        log.info("newLoginProcess kickLocalClient oldPod {} userId {} ret {}", oldPod, userId, ret);
                                                    });
                                        });
                            } else {
                                return Homo.result(true);

                            }
                        }
                )
                .nextDo(ret -> {
                    if (ret) {
                        return serviceStateMgr.setUserLinkedPod(userId, localProxyService.getHostName(), localProxyService.getPodIndex(), false)
                                .nextDo(linkPodIndex -> {
                                    log.info("newLoginProcess setUserLinkedPod success userId {} linkPodIndex {}", userId, linkPodIndex);
                                    return Homo.result(true);
                                });
                    } else {
                        log.error("login before process fail userId {}", userId);
                        return Homo.result(false);
                    }
                })
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
