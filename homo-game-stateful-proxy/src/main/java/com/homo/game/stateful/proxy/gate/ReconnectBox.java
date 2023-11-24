package com.homo.game.stateful.proxy.gate;

import com.homo.core.facade.gate.GateMessageHeader;
import com.homo.core.utils.spring.GetBeanUtil;
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
    String uid;
    //本端已经收到对端的包序号
    short clientSendSeq = GateMessageHeader.DEFAULT_RECV_SEQ;
    //本端已经发送给对端的包序号
    short innerSendSeq = GateMessageHeader.DEFAULT_SEND_SEQ;
//    short innerSendSeq = 0;

    CircularFifoQueue<CacheMsg> cacheQueue;
    ReconnectConfig reconnectConfig = GetBeanUtil.getBean(ReconnectConfig.class);

    public ReconnectBox() {

    }

    public void init(){
        short clientSendSeq = GateMessageHeader.DEFAULT_RECV_SEQ;
        //本端已经发送给对端的包序号
        short innerSendSeq = GateMessageHeader.DEFAULT_SEND_SEQ;
        this.cacheQueue = new CircularFifoQueue<>(reconnectConfig.getMaxSize());
        log.info("ReconnectBox init uid {} MaxSize {}", uid, reconnectConfig.getMaxSize());
    }

    //cache info Pair<当前缓存开始序列号,已缓存个数(0代表left val为下一个缓存序号)>
    public Pair<Short, Short> currentReconnectInfo() {
        Short beginCache = getCurrentSendSeq();
        Short count = new Integer(getSize()).shortValue();
        return new Pair<>(beginCache, count);
    }

    public Short getCurrentSendSeq() {
        Short beginCache = cacheQueue.isEmpty() ? getCycleSeq(innerSendSeq) : cacheQueue.get(0).innerSendSeq;
        return beginCache;
    }

    //转换用
    private CacheMsg doCacheMsg(String msgId, byte[] bytes, short sessionId, short serverSendSeq, short clientSendSeq) {
        this.innerSendSeq = serverSendSeq;
        this.clientSendSeq = clientSendSeq;
        CacheMsg cacheMsg = new CacheMsg(msgId, bytes, sessionId, serverSendSeq, clientSendSeq);
        cacheQueue.add(cacheMsg);
        log.info("doCacheMsg uid {} msg {} serverSendSeq {} clientSendSeq {}",uid,cacheMsg,serverSendSeq,clientSendSeq);
        if (cacheQueue.size() == cacheQueue.maxSize()) {
            log.error("doCacheMsg is full user {}  size {}", sessionId, cacheQueue.size());
        }
        return cacheMsg;
    }

    //消息发送用
    public boolean cacheMsg(String msgId, byte[] bytes, short sessionId) {
        if (!reconnectConfig.cacheFilter(msgId)) {
            return false;
        }
        innerSendSeq = getCycleSeq(innerSendSeq);
        CacheMsg cacheMsg = doCacheMsg(msgId, bytes, sessionId, innerSendSeq, clientSendSeq);
        log.info("server send cacheMsg user {} innerSendSeq {} cacheMsg {}", uid, innerSendSeq, cacheMsg);
        return true;
    }

    public Pair<Boolean, List<CacheMsg>> transfer(Integer clientCacheSendReq, Integer clientConfirmInnerSeq, Integer count) {
        Short clientCacheStartReqShort = clientCacheSendReq.shortValue();
        Short clientConfirmSeqShort = clientConfirmInnerSeq.shortValue();
        trimCache(clientConfirmSeqShort);//先清空已读的消息
        Short currentSendSeq = getCurrentSendSeq();
        Short msgCount = getMsgCount();
        //检查是否满足重连条件
        //clientConfirmSeq in cache //todo 待完善，先默认全部返回
        // 判断确认的包序是否在对方cache中,可以接上

        boolean inCache = inCache(clientConfirmSeqShort) ;
        List<CacheMsg> allCacheMsg = new ArrayList<>();
        if ( inCache) {
            allCacheMsg = getAllCacheMsg();
        }
        return new Pair<>(inCache, allCacheMsg);
    }

    public List<CacheMsg> getAllCacheMsg() {
        return new ArrayList<>(cacheQueue);
    }

    public List<CacheMsg> getCacheMsgList(Integer index,Integer count) {
        List<CacheMsg> cacheMsgList = new ArrayList<>();
        for (int i = index; i < index + count; i++) {
            if (cacheQueue.get(i)==null){
                break;
            }
            cacheMsgList.add(cacheQueue.get(i));
        }
        return cacheMsgList;
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
        if (clientConfirmSeq == 0){
            return;
        }
        if (inCache(clientConfirmSeq)) {
            CacheMsg poll = null;
            do {
                poll = cacheQueue.poll();
            } while (poll != null && poll.innerSendSeq != clientConfirmSeq);
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
        short beginInnerSeq = beginCacheMsg.getInnerSendSeq();
        short endInnerSeq = endCacheMsg.getInnerSendSeq();
        return inCache(clientConfirmSeq, beginInnerSeq, endInnerSeq);
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
        if (errorCode != TransferCacheResp.ErrorType.OK) {
            return false;
        }
        init();// 单点模式模拟重连
        List<TransferCacheResp.SyncMsg> syncMsgList = resp.getSyncMsgListList();
        Integer sendSeq = resp.getSendSeq();
        Integer recvSeq = resp.getRecvSeq();
        //接收缓存消息
        for (TransferCacheResp.SyncMsg syncMsg : syncMsgList) {
//            log.info("syncMsg uid {} syncMsg {}",uid,syncMsg);
            //先设置SyncMsg内的同步序号
            doCacheMsg(syncMsg.getMsgId(), syncMsg.getMsg().toByteArray(), new Integer(syncMsg.getSessionId()).shortValue(),
                    sendSeq.shortValue(), recvSeq.shortValue());
        }
        //但外层如果带序号，则覆盖里层序号
        if (sendSeq > 0) {
            this.innerSendSeq = sendSeq.shortValue();
        }
        if (recvSeq > 0) {
            this.clientSendSeq = recvSeq.shortValue();
        }
        return true;
    }


    public void clear() {
        cacheQueue.clear();
        clientSendSeq = GateMessageHeader.DEFAULT_RECV_SEQ;
        innerSendSeq = GateMessageHeader.DEFAULT_SEND_SEQ;
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

    public void updateMsgSeq(short clientSendSeq, short confirmServerSendSeq) {
        log.info("client send updateMsgSeq uid {} clientSendSeq {} confirmServerSendSeq {}", uid, clientSendSeq, confirmServerSendSeq);
        //忽略心跳包，业务包需要确认序号
        /**
         * 如果peerCurrentSendSeq < 0 表示是心跳包,不需要确认序号
         * 如果peerCurrentSendSeq >= 0 表示是业务包,需要确认序号
         * 心跳包也会带上客户端的确认序号
         * 目前约定sendReq从0开始
         */
        if (clientSendSeq >= 0) {
            if (getCycleSeq(this.clientSendSeq) != clientSendSeq) {
                // 接受序号必须是连续的
                log.error("userId {} receive req error ! recvReq {} peerSendSeq {} peerConfirmRecvSeq {}", uid, this.clientSendSeq, clientSendSeq, confirmServerSendSeq);
            }
            // 记录自己已经收到的消息序号
            this.clientSendSeq = clientSendSeq;
        }
        // 根据客户端确认的序号,清理缓存
        trimCache(confirmServerSendSeq);
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
