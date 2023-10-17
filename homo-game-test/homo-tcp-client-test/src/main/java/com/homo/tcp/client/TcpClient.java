package com.homo.tcp.client;

import com.homo.core.gate.tcp.handler.GateMessageDecode4Tcp;
import com.homo.core.gate.tcp.handler.GateMessageEncode4Tcp;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TcpClient {
    public String host = "127.0.0.1";
    public int port = 6666;
    public String routerMsgName;
    public String entityMsgName;
    ChannelFuture channelFuture;

    EventLoopGroup workGroup;

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
                ch.pipeline().addLast(new ClientHandler());
            }
        });
        try {
            channelFuture = bootstrap.connect(host, port).sync();
        } catch (InterruptedException e) {
            log.error("TcpClient connect e", e);
            workGroup.shutdownGracefully();
        }
    }
}
