package com.homo.tcp.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import com.homo.core.facade.gate.GateMessageHeader;
import com.homo.core.facade.gate.GateMessagePackage;
import com.homo.core.facade.gate.GateMessageType;
import com.homo.core.gate.tcp.handler.GateMessageDecode4Tcp;
import com.homo.core.gate.tcp.handler.GateMessageEncode4Tcp;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

@Slf4j
public class TcpClient {
    public String host;
    public int port;
    public String routerMsgName = "RouterMsgReq";
    public String entityMsgName = "onEntityCallForHttpProxy";
    public boolean autoConfirm = true;
    ChannelFuture channelFuture;
    public short clientSeq = -1;
    public short confirmSeq = 0;
    public short sessionId = 0;
    public int clientVersion = 1;
    EventLoopGroup workGroup;
    Map<String, Consumer<byte[]>> msgCallBackConsumerMap = new HashMap<>();

    Map<Short, Consumer<byte[]>> callbackConsumerMap = new HashMap<>();


    public TcpClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.sessionId = (short) new Random().nextInt(Short.MAX_VALUE); //todo 还没想好 预留
    }

    public void connect() {
        Bootstrap bootstrap = new Bootstrap();
        workGroup = new NioEventLoopGroup();
        bootstrap.group(workGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                log.info("TcpClient initChannel ch {}", ch);
                ch.pipeline().addLast(new GateMessageEncode4Tcp());
                ch.pipeline().addLast(new GateMessageDecode4Tcp());
                ch.pipeline().addLast(new ClientHandler(TcpClient.this));//匿名类传TcpClient.this
            }
        });
        try {
            /**
             * 建立连接 (connect) 的过程是 异步非阻塞 的，若不通过 sync() 方法阻塞主线程，
             * 等待连接真正建立，这时通过 channelFuture.channel () 拿到的 Channel 对象，
             * 并不是真正与服务器建立好连接的 Channel，也就没法将信息正确的传输给服务器端
             */
            channelFuture = bootstrap.connect(host, port).sync();
        } catch (InterruptedException e) {
            log.error("TcpClient connect e", e);
            workGroup.shutdownGracefully();
        }
    }

    public void routerMsg(String serviceName, String methodName, GeneratedMessageV3 msg,String callBackId,  Consumer<byte[]> consumer) {
        ClientRouterMsg routerMsg = ClientRouterMsg.newBuilder()
                .setSrcService(serviceName)
                .setMsgId(methodName)
                .addMsgContent(msg.toByteString())
                .build();
        baseCallMsg(routerMsgName, routerMsg.toByteString(), callBackId,consumer);
    }

    public void close() {
        try {
            channelFuture.channel().close().sync();
        } catch (InterruptedException e) {
            workGroup.shutdownGracefully();
        }
    }

    public void baseCallMsg(String methodName, ByteString data,String callBackId, Consumer<byte[]> consumer) {
        Msg.Builder builder = Msg.newBuilder().setMsgId(methodName);
        if (data != null) {
            builder.setMsgContent(data);
        }
        Msg msg = builder.build();
        GateMessagePackage gateMessagePackage = new GateMessagePackage(msg.toByteArray());
        GateMessageHeader header = gateMessagePackage.getHeader();
        msgCallBackConsumerMap.put(callBackId,consumer);
        if ("HeartBeatReq".equals(methodName)) {
            header.setType(GateMessageType.HEART_BEAT.ordinal());
        }else {
            clientSeq ++;
//            pushCallBack(clientSeq,consumer);
            header.setType(GateMessageType.PROTO.ordinal());
            header.setSendSeq(clientSeq);
            header.setSessionId(clientSeq);
            header.setRecvSeq(confirmSeq);
        }
        header.setVersion(clientVersion);
        channelFuture.channel().writeAndFlush(gateMessagePackage);
        log.info("sendMsg msgId_{} ", methodName);
    }

//    short pushCallBack(Short sessionId,Consumer<byte[]> consumer) {
//        callbackConsumerMap.put(sessionId, consumer);
//        return clientSeq;
//    }
}
