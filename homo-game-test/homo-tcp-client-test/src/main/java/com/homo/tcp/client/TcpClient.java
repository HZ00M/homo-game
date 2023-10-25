package com.homo.tcp.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import com.homo.core.facade.gate.GateMessageHeader;
import com.homo.core.gate.tcp.GateMessagePackage;
import com.homo.core.gate.tcp.GateMessageType;
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
import java.util.function.Consumer;

@Slf4j
public class TcpClient {
    public String host;
    public int port;
    public String routerMsgName = "RouterMsgReq";
    public String entityMsgName = "onEntityCallForHttpProxy";
    ChannelFuture channelFuture;
    short clientSeq = 0;

    int clientVersion = 1;
    EventLoopGroup workGroup;
    Map<String, Consumer<byte[]>> msgCallBackConsumerMap = new HashMap<>();

    Map<Short, Consumer<byte[]>> callbackConsumerMap = new HashMap<>();

    public TcpClient(String host, int port) {
        this.host = host;
        this.port = port;
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
            channelFuture = bootstrap.connect(host, port).sync();
        } catch (InterruptedException e) {
            log.error("TcpClient connect e", e);
            workGroup.shutdownGracefully();
        }
    }

    public void routerMsg(String serviceName, String methodName, GeneratedMessageV3 msg, Consumer<byte[]> consumer) {
        ClientRouterMsg routerMsg = ClientRouterMsg.newBuilder()
                .setSrcService(serviceName)
                .setMsgId(methodName)
                .addMsgContent(msg.toByteString())
                .build();
        baseCallMsg(routerMsgName, routerMsg.toByteString(), consumer);
    }

    public void baseCallMsg(String methodName, ByteString data, Consumer<byte[]> consumer) {
        Msg.Builder builder = Msg.newBuilder().setMsgId(methodName);
        if (data != null) {
            builder.setMsgContent(data);
        }
        Msg msg = builder.build();
        GateMessagePackage gateMessagePackage = new GateMessagePackage(msg.toByteArray());
        GateMessageHeader header = gateMessagePackage.getHeader();
        if (consumer != null) {
            short callbackId = pushCallBack(consumer);
            msgCallBackConsumerMap.put(methodName, consumer);
            header.setSendSeq(callbackId);
        } else {
            header.setSendSeq((short) 0);
        }
        header.setVersion(clientVersion);
        header.setType(GateMessageType.PROTO.ordinal());
        channelFuture.channel().writeAndFlush(gateMessagePackage);
        log.info("sendMsg msgId_{}, ... end", methodName);
    }

    short pushCallBack(Consumer<byte[]> consumer) {
        if (consumer != null) {
            clientSeq++;
            if (clientSeq >= Short.MAX_VALUE) {
                clientSeq = 1;
            }
            callbackConsumerMap.put(clientSeq, consumer);
        }
        return clientSeq;
    }
}
