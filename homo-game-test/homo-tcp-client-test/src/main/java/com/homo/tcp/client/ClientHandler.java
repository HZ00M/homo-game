package com.homo.tcp.client;

import com.homo.core.facade.gate.GateMessageHeader;
import com.homo.core.facade.gate.GateMessagePackage;
import com.homo.core.facade.gate.GateMessageType;
import io.homo.proto.client.Msg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class ClientHandler extends ChannelInboundHandlerAdapter {

    TcpClient tcpClient;

    public ClientHandler(TcpClient tcpClient){
        this.tcpClient = tcpClient;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.error("ClientHandler channelInactive id {}",ctx.channel().id());
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object source) throws Exception {
        try {

            GateMessagePackage recvMsg = (GateMessagePackage) source;
            GateMessageHeader header = recvMsg.getHeader();
            if (header.getType()== GateMessageType.HEART_BEAT.ordinal()){
                log.info("receiveMsg msgId_HeartBeatReq {}",ctx.channel().remoteAddress());
                return;
            }
            Msg msg = Msg.parseFrom(recvMsg.getBody());
            String msgId = msg.getMsgId();
            log.info("receiveMsg  msgId {} header {}", msgId,header);
            short sessionId = header.getSessionId();
            short sendSeq = header.getSendSeq();
            short recvSeq = header.getRecvSeq();
            if (sendSeq > tcpClient.confirmSeq && tcpClient.autoConfirm){
                tcpClient.confirmSeq = sendSeq;
            }
//            Consumer<byte[]> seqCallBack = popCallBack(sessionId);
//            if (seqCallBack != null) {
//                seqCallBack.accept(msg.getMsgContent().toByteArray());
//            }
            Consumer<byte[]> msgCallBack = tcpClient.msgCallBackConsumerMap.get(msgId);
            if (msgCallBack != null) {
                msgCallBack.accept(msg.getMsgContent().toByteArray());
            }
        } catch (Exception e) {
            log.error("ClientHandler error channelRead id {} e",ctx.channel().id(), e);
        } finally {
            ReferenceCountUtil.safeRelease(source);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("ClientHandler exceptionCaught id {}",ctx.channel().id(), cause);
        ctx.close();
    }

//    Consumer<byte[]> popCallBack(short seq) {
//        if (seq == 0) {
//            return null;
//        }
//        Consumer<byte[]> consumer = tcpClient.callbackConsumerMap.get(seq);
//        if (consumer == null) {
//            log.error("sessionId:{} mapped consumer not found!", seq);
//            return null;
//        }
//        return consumer;
//    }

}
