package com.homo.game.stateful.proxy.gate;

import com.google.protobuf.ByteString;
import com.homo.core.facade.gate.GateMessage;
import com.homo.core.gate.DefaultGateClient;
import com.homo.core.gate.DefaultGateServer;
import com.homo.core.utils.concurrent.queue.CallQueueMgr;
import com.homo.core.utils.concurrent.queue.CallQueueProducer;
import com.homo.core.utils.concurrent.queue.IdCallQueue;
import com.homo.core.utils.concurrent.schedule.HomoTimerMgr;
import com.homo.core.utils.concurrent.schedule.HomoTimerTask;
import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.spring.GetBeanUtil;
import com.homo.game.stateful.proxy.StatefulProxyService;
import com.homo.game.stateful.proxy.config.StatefulProxyProperties;
import com.homo.game.stateful.proxy.pojo.CacheMsg;
import io.homo.proto.client.*;
import javafx.util.Pair;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ProxyGateClient extends DefaultGateClient implements CallQueueProducer {
    @Getter
    public String uid;
    @Getter
    public String channelId;
    @Getter
    public State state = State.INIT;
    public short sessionId;
    public short sendSeqBeforeLogin = GateMessage.DEFAULT_SEND_SEQ;
    public short confirmSeqBeforeLogin = GateMessage.DEFAULT_RECV_SEQ;
    IdCallQueue queue = new IdCallQueue("gateClientQueue", 1000 * 11, IdCallQueue.DropStrategy.DROP_CURRENT_TASK);
    ReconnectBox reconnectBox;
    ReconnectConfig reconnectConfig;
    //重连后的目的proxy的podIndex
    public int transferDestPod = -1;
    public static Map<String, ReconnectBox> msg_cache = new HashMap<>();
    StatefulProxyProperties statefulProxyProperties = GetBeanUtil.getBean(StatefulProxyProperties.class);
    private HomoTimerTask timerTask;

    public ProxyGateClient(DefaultGateServer gateServer, String name) {
        super(gateServer, name);
    }

    public void init() {
        reconnectConfig = GetBeanUtil.getBean(ReconnectConfig.class);
        reconnectBox = msg_cache.computeIfAbsent(uid, k -> new ReconnectBox(uid, reconnectConfig));
        reconnectBox.clear();
        setState(ProxyGateClient.State.LOGIN);

    }

    public Homo<Boolean> toClient(Integer podId, ToClientReq req) {
        String msgId = req.getMsgType();
        ByteString msgContent = req.getMsgContent();
        Msg msg = Msg.newBuilder().setMsgId(msgId).setMsgContent(msgContent).build();
        return Homo.queue(queue, () -> {
            if (isLogin()) {
                //在线状态直接发送消息
                sendToClient(msg.toByteArray());
                //防止发送失败
                cacheMsg(msgId, msgContent.toByteArray());
            } else if (state == State.INACTIVE) {
                //掉线状态将数据缓存起来
//                sendToClient(msg.toByteArray());
                cacheMsg(msgId, msgContent.toByteArray());
            } else if (state == State.TRANSFERRED) {
                //转发  todo req能否直接透传待定
                GetBeanUtil.getBean(StatefulProxyService.class)
                        .forwardMsg(transferDestPod, req).start();
            }
            return Homo.result(true);
        }, () -> {
            log.error("toClient error podId {} req {}", podId, req);
        });
    }

    public void updateMsgSeq(short beforePeerSendSeq, short beforePeerConfirmSeq) {
        reconnectBox.updateMsgSeq(beforePeerSendSeq, beforePeerConfirmSeq);
    }

    @Override
    public Integer getQueueId() {
        if (uid != null) {
            return uid.hashCode() % CallQueueMgr.getInstance().getQueueCount();
        } else {
            return 0;
        }
    }

    public boolean cacheMsg(String msgId, byte[] bytes) {
        return reconnectBox.cacheMsg(msgId, bytes, sessionId);
    }


    public void setState(State state) {
        this.state = state;
    }

    public TransferCacheResp transfer(String uid, SyncInfo syncInfo, int fromPodId) {
        Integer cacheStartReq = syncInfo.getStartSeq();
        Integer clientConfirmSeq = syncInfo.getRecReq();
        int clientCount = syncInfo.getCount();
        log.info("transfer uid {} fromPodId {} cacheStartReq {} clientConfirmSeq {} clientCount {}", uid, fromPodId, cacheStartReq, clientConfirmSeq, clientCount);
        Pair<Boolean, List<CacheMsg>> transferMsg = reconnectBox.transfer(clientConfirmSeq, cacheStartReq, clientCount);
        if (!transferMsg.getKey().equals(true)) {
            return TransferCacheResp.newBuilder().build();
        }
        TransferCacheResp.Builder newBuilder = TransferCacheResp.newBuilder();
        for (CacheMsg cacheMsg : transferMsg.getValue()) {
            TransferCacheResp.SyncMsg syncMsg = TransferCacheResp.SyncMsg
                    .newBuilder()
                    .setMsgId(cacheMsg.getMsgId())
                    .setMsg(ByteString.copyFrom(cacheMsg.getBytes()))
                    .setSendSeq(cacheMsg.getSendSeq())
                    .setRecReq(cacheMsg.getRecvReq())
                    .setSessionId(cacheMsg.getSessionId())
                    .build();
            newBuilder.addSyncMsgList(syncMsg);
        }
        //改变状态
        transferDestPod = fromPodId;
        state = ProxyGateClient.State.TRANSFERRED;
        return newBuilder.build();
    }

    public Homo<Boolean> kickPromise(boolean offlineNotice) {
        if (state == State.CLOSED) {
            log.warn("uid {} already closed!", uid);
            return Homo.result(true);
        }
        Homo<Boolean> promise;
        if (offlineNotice) {
            KickUserMsg kickUserMsg = KickUserMsg.newBuilder().setKickReason("kickPromise").build();
            ToClientReq toClientReq = ToClientReq.newBuilder()
                    .setClientId(uid)
                    .setMsgType(KickUserMsg.class.getSimpleName())
                    .setMsgContent(kickUserMsg.toByteString())
                    .build();
            promise = sendToClientComplete(toClientReq.toByteArray());
        } else {
            promise = Homo.result(true);
        }
        promise.justThen(() -> {
            setState(State.CLOSED);
            ProxyGateClientMgr.removeFromValid(uid);
            try {
                close();
            } catch (Exception e) {
                log.error("kickPromise close error uid:{} handlerState:{}", uid, getState(), e);
            }
            return Homo.result(true);
        });

        return promise;
    }

    public void updateReconnectInfo(short sendSeq, short recvSeq, short sessionId) {
        this.sessionId = sessionId;//todo 待确认
        if (isLogin()) {
            updateMsgSeq(sendSeq, recvSeq);
        } else {
            //未登录时,记录下来,登录后再更新
            sendSeqBeforeLogin = sendSeq >= 0 ? sendSeq : sendSeqBeforeLogin;
            confirmSeqBeforeLogin = recvSeq >= 0 ? recvSeq : confirmSeqBeforeLogin;
        }
    }


    public enum State {
        INIT, //初始状态
        LOGIN, //已登录
        RECONNECTED,//已重连
        TRANSFERRED,//已转移
        INACTIVE,//掉线状态
        CLOSED; //已经关闭
    }

    public boolean isLogin() {
        return state == State.LOGIN || state == State.RECONNECTED;
    }

    //接收先去连接对象gateClient销毁前的缓存包
    public boolean recvTransferMsgs(TransferCacheResp resp) throws Exception {
        return reconnectBox.recvTransferMsgs(resp);
    }

    public Homo<Boolean> unRegisterClient(boolean offlineNotify, String reason) {
        if (state == State.INIT) {
            log.info("no user tcp close handler:{}", this);
            state = State.CLOSED;
            return Homo.result(true);
        }
        if (state == State.CLOSED || state == State.INACTIVE) {
            log.warn("unRegisterClient uid {} already closed!", uid);
            return Homo.result(true);
        }
        ParameterMsg parameterMsg = ParameterMsg.newBuilder().setChannelId(channelId).setUserId(uid).build();
        StateOfflineRequest offlineRequest = StateOfflineRequest.newBuilder().setReason(reason).setTime(System.currentTimeMillis()).build();
        if (offlineNotify) {
            for (String notifyService : statefulProxyProperties.getClientOfflineNotifyServiceSet()) {
                //todo 通知服务操作
            }
        }
        state = State.CLOSED;
        ProxyGateClientMgr.removeFromValid(uid);
        return Homo.result(true);
    }

    @Override
    public void onClose(String reason) {
        log.info("GateClientImpl onClose reason {}", reason);
        if (isLogin()) {
            state = State.INACTIVE;
            timerTask = HomoTimerMgr.getInstance().once(() -> {
                        Homo.queue(queue, () -> {
                            log.info("开始销毁 uid {} handlerState {}", uid, state);
                            return unRegisterClient(true, reason);
                        }, null).start();
                    },
                    statefulProxyProperties.getClientOfflineDelayCloseSecond() * 1000);

        } else {
            unRegisterClient(false, reason).start();
        }
    }
}
