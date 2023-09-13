package com.homo.game.stateful.proxy.gate;

import com.homo.core.facade.gate.GateMessage;
import com.homo.game.stateful.proxy.pojo.CacheMsg;
import io.homo.proto.client.TransferCacheResp;
import javafx.util.Pair;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
public class ReconnectBox {
    String userId;
    //本端已经收到对端的包序号
    short recvSeq = GateMessage.DEFAULT_RECV_SEQ;
    //本端已经发送给对端的包序号
    short sendSeq = GateMessage.DEFAULT_SEND_SEQ;
    CircularFifoQueue<CacheMsg> cacheQueue;
    ReconnectConfig reconnectConfig;

    public ReconnectBox(String uid, ReconnectConfig reconnectConfig) {
        this.userId = uid;
        this.reconnectConfig = reconnectConfig;
        this.cacheQueue = new CircularFifoQueue<>(reconnectConfig.getMaxSize());
    }

    //cache info Pair<当前缓存开始序列号,已缓存个数(0代表left val为下一个缓存序号)>
    public Pair<Short, Short> currentReconnectInfo() {
        Short beginCache = getCurrentSendSeq();
        Short count = new Integer(getSize()).shortValue();
        return new Pair<>(beginCache, count);
    }

    public Short getCurrentSendSeq() {
        Short beginCache = cacheQueue.isEmpty() ? getCycleSeq(sendSeq) : cacheQueue.get(0).sendSeq;
        return beginCache;
    }

    //转换用
    public void cacheMsg(String msgId, byte[] bytes, short sessionId, short sendSequence, short recvSequence) {
        this.sendSeq = sendSequence;
        this.recvSeq = recvSequence;
        cacheQueue.add(new CacheMsg(msgId, bytes, sessionId, sendSequence, recvSequence));
        if(cacheQueue.size() == cacheQueue.maxSize()){
            log.error("cacheMsg is full user {}  size {}",sessionId,cacheQueue.size());
        }
    }

    //消息发送用
    public boolean cacheMsg(String msgId, byte[] bytes, short sessionId) {
        if (!reconnectConfig.cacheFilter(msgId)) {
            return false;
        }
        sendSeq = getCycleSeq(sendSeq);
        CacheMsg cacheMsg = new CacheMsg(msgId, bytes, sessionId, sendSeq, recvSeq);
        cacheQueue.add(cacheMsg);
        if (cacheQueue.size() == cacheQueue.maxSize()) {
            log.error("cacheMsg cache is full user {} size {}", userId, cacheQueue.size());
        }
        return true;
    }

    public Pair<Boolean, List<CacheMsg>> transfer(Integer cacheStartReq, Integer clientConfirmSeq, Integer count) {
        Short clientCacheStartReqShort = cacheStartReq.shortValue();
        Short clientConfirmSeqShort = clientConfirmSeq.shortValue();

        trimCache(clientConfirmSeqShort);
        Short currentSendSeq = getCurrentSendSeq();
        Short msgCount = getMsgCount();
        //检查是否满足重连条件//判断确认的包序是否在对方cache中,或者可以接上
        boolean inCache = inCache(clientConfirmSeqShort);
        List<CacheMsg> allCacheMsg = new ArrayList<>();
        if (inCache) {
            allCacheMsg = getAllCacheMsg();
        }
        return new Pair<>(inCache, allCacheMsg);
    }

    public List<CacheMsg> getAllCacheMsg() {
        return new ArrayList<>(cacheQueue);
    }

    public CacheMsg getCacheMsg(int index) {
        return cacheQueue.get(index);
    }

    /**
     * 根据客户端已确认的序号,清理缓存
     *
     * @param clientConfirmSeq
     */
    public void trimCache(Short clientConfirmSeq) {
        if (inCache(clientConfirmSeq)) {
            CacheMsg poll = null;
            do {
                poll = cacheQueue.poll();
            } while (poll != null && poll.sendSeq != clientConfirmSeq);
        }
    }

    private boolean inCache(short clientConfirmSeq) {
        if (cacheQueue.size() == 0 || clientConfirmSeq < 0) {
            return false;
        }
        CacheMsg beginCacheMsg = cacheQueue.get(0);
        CacheMsg endCacheMsg = cacheQueue.get(cacheQueue.size() - 1);
        if (beginCacheMsg == null) {
            return false;
        }
        short beginSeq = beginCacheMsg.getSendSeq();
        short endSeq = endCacheMsg.getSendSeq();
        return inCache(clientConfirmSeq, beginSeq, endSeq);
    }

    /**
     * 判断确认的包序是否在对方cache中, 会基于short做循环判断
     *
     * @param seq      需要判断的序号
     * @param beginSeq cache的开始序号
     * @param endSeq   cache的结束序号
     * @return 是否在cache中
     */
    public static boolean inCache(short seq, short beginSeq, short endSeq) {
        if (beginSeq <= endSeq) {
            return seq >= beginSeq && seq <= endSeq;
        }
        return !(seq > endSeq && seq < beginSeq);//todo 待验证
    }

    public boolean recvTransferMsgs(TransferCacheResp resp) {
        TransferCacheResp.ErrorType errorCode = resp.getErrorCode();
        if (errorCode != TransferCacheResp.ErrorType.OK){
            return false;
        }
        List<TransferCacheResp.SyncMsg> syncMsgList = resp.getSyncMsgListList();
        Integer sendSeq = resp.getSendSeq();
        Integer recvSeq = resp.getRecvSeq();
        //接收缓存消息
        for (TransferCacheResp.SyncMsg syncMsg : syncMsgList) {
            //先设置SyncMsg内的同步序号
            cacheMsg(syncMsg.getMsgId(),syncMsg.getMsg().toByteArray(),new Integer(syncMsg.getSessionId()).shortValue(),
                    sendSeq.shortValue() , recvSeq.shortValue());
        }
        //但外层如果带序号，则覆盖里层序号
        if (sendSeq > 0){
            this.sendSeq = sendSeq.shortValue();
        }
        if (recvSeq > 0){
            this.recvSeq = recvSeq.shortValue();
        }
        return true;
    }



    public void clear() {
        cacheQueue.clear();
        recvSeq = GateMessage.DEFAULT_RECV_SEQ;
        sendSeq = GateMessage.DEFAULT_SEND_SEQ;
    }

    public CacheMsg getCacheMsg(Integer index) {
        return cacheQueue.get(index);
    }


    public Short getMsgCount() {
        return Integer.valueOf(getSize()).shortValue();
    }

    public int getSize() {
        return cacheQueue.size();
    }

    public void updateMsgSeq(short peerSendSeq, short peerConfirmRecvSeq) {
        if (log.isDebugEnabled()) {
            log.debug("updateCache uid:{} peerCurrentSendSeq {} peerConfirmRecvSeq {}", userId, peerSendSeq, peerConfirmRecvSeq);
        }
        //忽略心跳包，业务包需要确认序号
        /**
         * 如果peerCurrentSendSeq < 0 表示是心跳包,不需要确认序号
         * 如果peerCurrentSendSeq >= 0 表示是业务包,需要确认序号
         * 心跳包也会带上客户端的确认序号
         */
        if (peerSendSeq >= 0) {
            if (getCycleSeq(recvSeq) != peerSendSeq) {
                // 接受序号必须是连续的
                log.error("userId {} receive req error ! recvReq {} peerSendSeq {}", userId, recvSeq, peerSendSeq);
            }
            // 记录自己已经收到的消息序号
            recvSeq = peerSendSeq;
        }
        // 根据客户端确认的序号,清理缓存
        trimCache(peerConfirmRecvSeq);
    }

    /**
     * 获得递增值, 超过short最大值就归零
     *
     * @param num 递增的数
     * @return 递增后的数
     **/
    public static short getCycleSeq(short num) {

        Assert.isTrue(num >= -1, "getIncrValue num  " + num + " must be greater than or equal to -1");
        if (num == -1) {
            return 0;
        }
        return getIncrValue(num, (short) 1);
    }

    /**
     * 获得大于0的循环递增值
     *
     * @param num   递增的数
     * @param delta 递增的值
     * @return 递增后的数
     **/
    public static short getIncrValue(short num, short delta) {
        Assert.isTrue(num >= 0, "getIncrValue num " + num + "must be greater than or equal to 0");
        Assert.isTrue(delta > 0, "getIncrValue delta " + delta + " must be greater than 0");
        short leftSpace = (short) (Short.MAX_VALUE - num);
        if (delta > leftSpace) {
            num = (short) (delta - leftSpace - 1);
        } else {
            num += delta;
        }
        return num;
    }
}
