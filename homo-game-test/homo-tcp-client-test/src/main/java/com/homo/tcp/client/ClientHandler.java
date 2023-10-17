package com.homo.tcp.client;

import com.homo.core.facade.gate.GateMessage;
import com.homo.core.gate.tcp.GateMessagePackage;
import io.homo.proto.client.Msg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class ClientHandler extends ChannelInboundHandlerAdapter {

    Map<String, Consumer<byte[]>> msgCallBackConsumerMap = new HashMap<>();

    Map<Short, Consumer<byte[]>> callbackConsumerMap = new HashMap<>();

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.error("ClientHandler channelInactive");
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object source) throws Exception {
        try {

            GateMessagePackage recvMsg = (GateMessagePackage) source;
            log.info("ClientHandler channelRead recvMsg {}", recvMsg);
            GateMessage.Header header = recvMsg.getHeader();
            Msg msg = Msg.parseFrom(recvMsg.getBody());
            String msgId = msg.getMsgId();
            short clientSeq = header.getClientSeq();
            Consumer<byte[]> seqCallBack = popCallBack(clientSeq);
            if (seqCallBack != null) {
                seqCallBack.accept(msg.getMsgContent().toByteArray());
            }
            Consumer<byte[]> msgCallBack = msgCallBackConsumerMap.get(msgId);
            if (msgCallBack != null) {
                msgCallBack.accept(msg.getMsgContent().toByteArray());
            }
        } catch (Exception e) {
            log.error("ClientHandler channelRead e", e);
        } finally {
            ReferenceCountUtil.safeRelease(source);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("ClientHandler exceptionCaught", cause);
        ctx.close();
    }

    Consumer<byte[]> popCallBack(short seq) {
        if (seq == 0) {
            return null;
        }
        Consumer<byte[]> consumer = callbackConsumerMap.get(seq);
        if (consumer == null) {
            log.error("sessionId:{} mapped consumer not found!", seq);
            return null;
        }
        return consumer;
    }

}
