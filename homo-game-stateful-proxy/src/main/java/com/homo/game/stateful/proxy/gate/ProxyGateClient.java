package com.homo.game.stateful.proxy.gate;

import com.google.protobuf.ByteString;
import com.homo.core.facade.gate.GateMessageHeader;
import com.homo.core.gate.DefaultGateClient;
import com.homo.core.gate.DefaultGateServer;
import com.homo.core.utils.concurrent.queue.CallQueue;
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
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 提供对断线重连的支持
 */
@Slf4j
@ToString(exclude = {"queue", "reconnectBox", "delayDestroyTask", "statefulProxyProperties"})
public class ProxyGateClient extends DefaultGateClient implements CallQueueProducer {
    public String uid;
    public String channelId;
    private State state = State.INIT;
    public short sessionId;
    public short clientSendSeqBeforeLogin = GateMessageHeader.DEFAULT_SEND_SEQ;
    public short confirmSeverSendSeqBeforeLogin = GateMessageHeader.DEFAULT_RECV_SEQ;
    IdCallQueue queue = new IdCallQueue("gateClientQueue", 1000 * 11, IdCallQueue.DropStrategy.DROP_CURRENT_TASK, 0);
    ReconnectBox reconnectBox = new ReconnectBox();
    //重连后的目的proxy的podIndex
    public int transferDestPod = -1;
    //    public static Map<String, ReconnectBox> msg_cache = new HashMap<>();
    private StatefulProxyProperties statefulProxyProperties = GetBeanUtil.getBean(StatefulProxyProperties.class);
    private HomoTimerTask delayDestroyTask;

    public ProxyGateClient(DefaultGateServer gateServer, String name) {
        super(gateServer, name);
    }

    public void newLogin(String uid) {
        this.reconnectBox.init();
        this.state = State.LOGIN;
        this.uid = uid;
        this.reconnectBox.setUid(uid);
        this.reconnectBox.updateMsgSeq(clientSendSeqBeforeLogin, confirmSeverSendSeqBeforeLogin);
        ProxyGateClientMgr.bindGate(uid, this);//必须登陆完成后调用  因为可能有其他登陆的gateClient产生，必须保证绑定的唯一性
        log.info("newLogin uid {} client {} ", uid, this);
    }

    public void reconnect(String uid) {
        reconnectBox.init();
        this.uid = uid;
        this.state = State.RECONNECTED;
        this.reconnectBox.setUid(uid);
        this.reconnectBox.updateMsgSeq(clientSendSeqBeforeLogin, confirmSeverSendSeqBeforeLogin);
        ProxyGateClientMgr.bindGate(uid, this);//必须登陆完成后调用  因为可能有其他登陆的gateClient产生，必须保证绑定的唯一性
        log.info("reconnect uid {} client {} ", uid, this);
    }

    public Homo<Boolean> toClient(Integer podId, ToClientReq req) {
        String msgId = req.getMsgType();
        ByteString msgContent = req.getMsgContent();
        Msg msg = Msg.newBuilder().setMsgId(msgId).setMsgContent(msgContent).build();
        return Homo.queue(queue, () -> {
            if (isLogin()) {
                //在线状态直接发送消息
                sendToClient(req.getMsgType(), msg.toByteArray()).start();
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

//    public void updateMsgSeq(short beforePeerSendSeq, short beforePeerConfirmSeq, Msg msg) {
//        if (msg != null) {
//            cacheMsg(msg.getMsgId(), msg.toByteArray());
//        }
//        reconnectBox.updateMsgSeq(beforePeerSendSeq, beforePeerConfirmSeq);
//    }


    @Override
    public Homo<Boolean> sendToClient(String msgId, byte[] msg) {
        return super.sendToClient(msgId, msg, sessionId, getInnerSendSeq(), getClientSendSeq());
    }

    public Short getClientSendSeq() {
        return reconnectBox.clientSendSeq;
    }

    public Short getInnerSendSeq() {
        return reconnectBox.innerSendSeq;
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


    public void setState(State state, String reason) {
        log.info("setState reason {}  uid {} beforeState {} state {} ", reason, uid, this.state, state);
        this.state = state;
    }

    public TransferCacheResp transfer(String uid, SyncInfo syncInfo, int fromPodId) {
        Integer clientConfirmSeq = syncInfo.getRecReq();
        Integer cacheStartReq = syncInfo.getStartSeq();
        int clientCount = syncInfo.getCount();
        log.info("transfer uid {} fromPodId {} cacheStartReq {} clientConfirmSeq {} clientCount {}", uid, fromPodId, cacheStartReq, clientConfirmSeq, clientCount);
        if (state == State.LOGIN || state == State.RECONNECTED || state == State.INACTIVE) {
            Pair<Boolean, List<CacheMsg>> transferMsg = reconnectBox.transfer(clientConfirmSeq, cacheStartReq, clientCount);
            if (!transferMsg.getKey().equals(true)) {
                return TransferCacheResp.newBuilder().build();
            }
            TransferCacheResp.Builder newBuilder = TransferCacheResp.newBuilder();
            for (CacheMsg cacheMsg : transferMsg.getValue()) {
                //重构消息
                TransferCacheResp.SyncMsg syncMsg = TransferCacheResp.SyncMsg
                        .newBuilder()
                        .setMsgId(cacheMsg.getMsgId())
                        .setMsg(ByteString.copyFrom(cacheMsg.getBytes()))
                        .setSendSeq(cacheMsg.getInnerSendSeq())
                        .setRecReq(cacheMsg.getClientSendReq())
                        .setSessionId(cacheMsg.getSessionId())
                        .build();
                newBuilder.addSyncMsgList(syncMsg);
            }
            newBuilder.setSendSeq(getInnerSendSeq());
            newBuilder.setRecvSeq(getClientSendSeq());
            //改变状态
            transferDestPod = fromPodId;
            state = ProxyGateClient.State.TRANSFERRED;
            return newBuilder.build();
        } else {
            return TransferCacheResp.newBuilder().setErrorCode(TransferCacheResp.ErrorType.ERROR).build();
        }
    }

    public Homo<Boolean> kickPromise(boolean offlineNotice) {
        log.info("kickPromise start uid {} offlineNotice {} state {}", uid, offlineNotice, state);
        if (state == State.CLOSED) {
            log.warn("uid {} already closed!", uid);
            return Homo.result(true);
        }

//        if (offlineNotice) {
//            KickUserMsg kickUserMsg = KickUserMsg.newBuilder().setKickReason("kickPromise").build();
//            ToClientReq toClientReq = ToClientReq.newBuilder()
//                    .setClientId(uid)
//                    .setMsgType(KickUserMsg.class.getSimpleName())
//                    .setMsgContent(kickUserMsg.toByteString())
//                    .build();
//            sendToClientComplete(KickUserMsg.class.getSimpleName(), toClientReq.toByteArray()).start();
//
//        }
        if (state == State.INACTIVE) {//掉线状态不需要断开端口连接
            delayDestroyTask.cancel();
        } else {
            closeClient();
        }
        return Homo.result(true);
    }

    public void closeClient() {
        log.info("closeClient start uid {} name {}", uid, name());
        try {
            close();
        } catch (Exception e) {
            log.error("closeClient close error uid:{} handlerState:{}", uid, getState(), e);
        }
        log.info("closeClient success client {}", this);
    }


    public void syncMsgSeq(short clientSendSeq, short confirmServerSendSeq, short sessionId) {
        this.sessionId = sessionId;
        if (isLogin()) {
            reconnectBox.updateMsgSeq(clientSendSeq, confirmServerSendSeq);
//            log.info("syncMsgSeq uid {}  clientSendSeq {} confirmServerSendSeq {}   " +
//                            "reconnectBox.getClientSendSeq {} reconnectBox.getInnerSendSeq {}",
//                    uid, clientSendSeq, confirmServerSendSeq, reconnectBox.getClientSendSeq(), reconnectBox.getInnerSendSeq());
        } else {
            //未登录时,记录下来,登录后再更新
            clientSendSeqBeforeLogin = clientSendSeq >= 0 ? clientSendSeq : clientSendSeqBeforeLogin;
            confirmSeverSendSeqBeforeLogin = confirmServerSendSeq >= 0 ? confirmServerSendSeq : confirmSeverSendSeqBeforeLogin;
//            log.info("syncMsgSeq uid {} clientSendSeqBeforeLogin {} confirmSeverSendSeqBeforeLogin {} clientSendSeq {} confirmServerSendSeq {}   ",
//                    uid, clientSendSeqBeforeLogin, confirmSeverSendSeqBeforeLogin, clientSendSeq, confirmServerSendSeq);
        }

    }

//    public void updateReconnectInfo(short sendSeq, short recvSeq,String sessionId, Msg msg) {
//        if (isLogin()) {
//            if (msg != null) {
//                cacheMsg(msg.getMsgId(), msg.toByteArray());
//            }
//            reconnectBox.updateMsgSeq(sendSeq, recvSeq);
//        } else {
//            //未登录时,记录下来,登录后再更新
//            sendSeqBeforeLogin = sendSeq >= 0 ? sendSeq : sendSeqBeforeLogin;
//            confirmSeqBeforeLogin = recvSeq >= 0 ? recvSeq : confirmSeqBeforeLogin;
//            log.info("updateReconnectInfo no login yet clientName {} sendSeqBeforeLogin {} confirmSeqBeforeLogin {}", name(), sendSeqBeforeLogin, confirmSeqBeforeLogin);
//            cacheMsg(msg.getMsgId(), msg.toByteArray());
//        }
//    }


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
            setState(State.CLOSED, "unRegisterClient");
            return Homo.result(true);
        }
        if (state == State.CLOSED || state == State.INACTIVE) {
            log.warn("unRegisterClient uid {} already closed!", uid);
            return Homo.result(true);
        }
//         todo 通知服务操作
//        ParameterMsg parameterMsg = ParameterMsg.newBuilder().setChannelId(channelId).setUserId(uid).build();
//        StateOfflineRequest offlineRequest = StateOfflineRequest.newBuilder().setReason(reason).setTime(System.currentTimeMillis()).build();
//        if (offlineNotify) {
//            for (String notifyService : statefulProxyProperties.getClientOfflineNotifyServiceSet()) {
//
//            }
//        }
        setState(State.CLOSED, "unRegisterClient");
        ProxyGateClientMgr.unBindGate(uid, this);
        log.info("unRegisterClient offlineNotify {} reason {} client {}", offlineNotify, reason, this);
        return Homo.result(true);
    }

    @Override
    public void onClose(String reason) {
        log.info("GateClientImpl onClose reason {} clientHashCode {}", reason, this.hashCode());
        if (isLogin()) {
            setState(State.INACTIVE, "onClose " + reason);
            CallQueue currentCallQueue = CallQueueMgr.getInstance().getQueueByUid(getUid());
            delayDestroyTask = HomoTimerMgr.getInstance().once("gateClientClose_" + getUid(), currentCallQueue, () -> {
                        Homo.queue(queue, () -> {
                            log.info("delayDestroyTask run uid {} handlerState {} clientHashCode {}", uid, state, this.hashCode());
                            return unRegisterClient(true, reason);
                        }, null).start();
                    },
                    statefulProxyProperties.getClientCloseRemoveDelayMillisecond() * 1000);

        } else {
            //未登陆不会有断线重连需求  直接移除绑定
            setState(State.CLOSED, reason);
            unRegisterClient(false, reason).start();
        }
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
        log.info("gateClient setUid userId {}", uid);
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public State getState() {
        return state;
    }
}
