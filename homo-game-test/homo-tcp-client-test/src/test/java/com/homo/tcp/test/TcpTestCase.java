package com.homo.tcp.test;

import com.google.protobuf.InvalidProtocolBufferException;
import com.homo.tcp.client.GameStatefulProxyTestApplication;
import com.homo.tcp.client.TcpClient;
import io.homo.proto.client.LoginMsgReq;
import io.homo.proto.client.LoginMsgResp;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.function.Consumer;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = GameStatefulProxyTestApplication.class)
public class TcpTestCase {
    TcpClient tcpClient;
    TcpClient watchClient;
    String watchUserId = "watcher_user1";
    String channelId = "1000";
    String STATE_GAME_PROXY_IP = "127.0.0.1";
    Integer STATE_GAME_PROXY_PORT = 666;

    @BeforeAll
    public void setup() {
        watchClient  = new TcpClient(STATE_GAME_PROXY_IP, STATE_GAME_PROXY_PORT);
        watchClient.connect();

    }

    @Test
    public void testLogin() throws Exception {
        log.info("testLogin start");
        login(watchUserId);
    }

    @Test
    public void login(String userId) throws Exception {
        LoginMsgReq req = LoginMsgReq.newBuilder()
                .setToken("token")
                .setChannelId("1")
                .setUserId(userId)
                .build();
        watchClient.baseCallMsg("LoginMsgReq",req.toByteString(),login);
        Thread.currentThread().join();
    }


    public Consumer<byte[]> login = new Consumer<byte[]>() {
        @Override
        public void accept(byte[] bytes) {
            try {
                LoginMsgResp resp = LoginMsgResp.parseFrom(bytes);
                log.info("login accept resp {}", resp);
            } catch (InvalidProtocolBufferException e) {
                log.error("login accept e", e);
            }
        }
    };

}
