package com.homo.tcp.test;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.homo.core.facade.ability.IEntityService;
import com.homo.game.login.proto.Auth;
import com.homo.game.login.proto.Response;
import com.homo.tcp.client.GameStatefulProxyTestApplication;
import com.homo.tcp.client.TcpClient;
import io.homo.proto.client.*;
import io.homo.proto.entity.EntityRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Random;
import java.util.function.Consumer;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = GameStatefulProxyTestApplication.class)
public class TcpTestCase {
    TcpClient watchClient;
    String watchUserId ;
    String channelId = "1000";
    String STATE_GAME_PROXY_IP = "127.0.0.1";
    Integer STATE_GAME_PROXY_PORT = 666;
    String token = "token";
    String appVersion = "1";
    short sessionId = 1;
    Random random;
    String entityCallMethodName = "entityCall";
    Thread beatHeardThread;
    @BeforeAll
    public void setup() {
        watchClient = new TcpClient(STATE_GAME_PROXY_IP, STATE_GAME_PROXY_PORT);
        watchClient.connect();
        random = new Random();
        watchUserId = "user_17";
        beatHeardThread = buildHeartBeatThread();
        beatHeardThread.start();
    }

    public Thread buildHeartBeatThread() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()){
                    try{
                        sendHeartMsg();
                    }catch (InterruptedException e) {
                        log.error("sendHeartMsg e", e);
                        Thread.currentThread().interrupt();
                    }
                }
                log.info("Thread.currentThread().isInterrupted() end");
            }
        };
        Thread thread = new Thread(runnable);
        return thread;
    }

    public void sendHeartMsg() throws InterruptedException {
        while (true) {
            watchClient.baseCallMsg("HeartBeatReq", ByteString.EMPTY,null,null);
            Thread.sleep(60000);
        }

    }

    @Test
    public void testLoginAndQueryUserInfo() throws Exception {
        log.info("testLogin start");
        login();
        Thread.sleep(1000);
        entityCall();
        Thread.sleep(1000);
        queryUserInfo();
        Thread.currentThread().join();
    }

    @Test
    public void testCycleSendMsg() throws Exception {
        login();
        while (true){
            Thread.sleep(5000);
            entityCall();
            Thread.sleep(10000);
            queryUserInfo();
            Thread.sleep(10000);
        }
    }

    @Test
    public void reConnectTest() throws Exception {
        watchClient.autoConfirm = false;
        login();
        Thread.sleep(5000);
        for (int i = 0 ;i < 2;i++){
            entityCall();
            Thread.sleep(500);
            queryUserInfo();
            Thread.sleep(500);
        }
        relogin();
        Thread.sleep(1000);
        transfer();
        for (int i = 0 ;i < 2;i++){
            entityCall();
            Thread.sleep(500);
            queryUserInfo();
            Thread.sleep(500);
        }
        Thread.currentThread().join(100000);
    }

    @Test
    public void login() throws Exception {
        LoginMsgReq req = LoginMsgReq.newBuilder()
                .setToken(token)
                .setChannelId(channelId)
                .setUserId(watchUserId)
                .setAppVersion("1")
                .build();
        watchClient.baseCallMsg("LoginMsgReq", req.toByteString(),LoginMsgResp.class.getSimpleName(), login);
    }

    @Test
    public void relogin() throws Exception {
        watchClient.close();
        beatHeardThread.interrupt();
        setup();
        LoginMsgReq req = LoginMsgReq.newBuilder()
                .setToken(token)
                .setChannelId(channelId)
                .setUserId(watchUserId)
                .setAppVersion("1")
                .build();
//        SyncInfo syncInfo = SyncInfo.newBuilder().setStartSeq(watchClient.clientSeq).setRecReq(watchClient.confirmSeq).setCount(50).build();
        LoginAndSyncReq loginAndSyncReq = LoginAndSyncReq.newBuilder()
                .setLoginMsgReq(req)
//                .setSyncInfo(syncInfo)
                .build();
        watchClient.baseCallMsg("LoginAndSyncReq", loginAndSyncReq.toByteString(), LoginAndSyncResp.class.getSimpleName(),relogin);
    }

    @Test
    public void transfer() throws Exception {
        SyncInfo syncInfo = SyncInfo.newBuilder().setStartSeq(0).setRecReq(watchClient.confirmSeq).setCount(50).build();
        TransferCacheReq req = TransferCacheReq.newBuilder()
                .setUserId(watchUserId)
                .setSyncInfo(syncInfo)
                .build();

        watchClient.baseCallMsg("TransferCacheReq", req.toByteString(),TransferCacheResp.class.getSimpleName(), transfer);
    }

    @Test
    public void queryUserInfo() throws Exception {
        log.info("router start");
        Auth auth = Auth.newBuilder().setUserId(watchUserId).build();
        ClientRouterMsg clientRouterMsg = ClientRouterMsg.newBuilder()
                .setSrcService("login-service-grpc")
                .setUserId(watchUserId)
                .setMsgId("queryUserInfo")
                .setChannelId(channelId)
                .addMsgContent(auth.toByteString())
                .setToken(token)
                .build();
        sendMsg(clientRouterMsg, Response.class.getSimpleName(),new Consumer<byte[]>() {
            @Override
            public void accept(byte[] bytes) {
                try {
                    Msg msg = Msg.parseFrom(bytes);
                    Response response = Response.parseFrom(msg.getMsgContent());
                    log.info("router accept resp {}", response);
                } catch (InvalidProtocolBufferException e) {
                    log.error("auth accept e", e);
                }
            }
        });
//        Thread.currentThread().join();
    }

    @Test
    public void entityCall() throws Exception {
        log.info("router start");
        TestReq testReq = TestReq.newBuilder().setUserId(watchUserId).build();
        EntityRequest entityRequest = EntityRequest.newBuilder().setType("user").setId(watchUserId).setFunName("test").setSrcName("tcpClient").addContent(testReq.toByteString()).build();
        ClientRouterMsg clientRouterMsg = ClientRouterMsg.newBuilder()
//                .setSrcService("login-service-grpc")
                .setEntityType("user")
                .setUserId(watchUserId)
                .setMsgId(IEntityService.default_entity_call_method)
                .setChannelId(channelId)
                .addMsgContent(entityRequest.toByteString())
                .setToken(token)
                .build();
        sendMsg(clientRouterMsg, Response.class.getSimpleName(),new Consumer<byte[]>() {
            @Override
            public void accept(byte[] bytes) {
                try {
                    Response response = Response.parseFrom(bytes);
                    log.info("router accept resp {}", response);
                } catch (InvalidProtocolBufferException e) {
                    log.error("auth accept e", e);
                }
            }
        });
//        Thread.currentThread().join();
    }


    public void sendMsg(GeneratedMessageV3 protoMsg, String callBackId, Consumer<byte[]> callback){
        watchClient.baseCallMsg(protoMsg.getClass().getSimpleName(), protoMsg.toByteString(),callBackId, callback);
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

    public Consumer<byte[]> relogin = new Consumer<byte[]>() {
        @Override
        public void accept(byte[] bytes) {
            try {
                LoginAndSyncResp resp = LoginAndSyncResp.parseFrom(bytes);
                log.info("relogin accept resp {}", resp);
            } catch (InvalidProtocolBufferException e) {
                log.error("relogin accept e", e);
            }
        }
    };

    public Consumer<byte[]> transfer = new Consumer<byte[]>() {
        @Override
        public void accept(byte[] bytes) {
            try {
                TransferCacheResp resp = TransferCacheResp.parseFrom(bytes);
                watchClient.clientSeq = Integer.valueOf(resp.getRecvSeq()).shortValue();
                watchClient.confirmSeq = Integer.valueOf(resp.getSendSeq()).shortValue();
                log.info("transfer accept resp {}", resp);
            } catch (InvalidProtocolBufferException e) {
                log.error("relogin accept e", e);
            }
        }
    };

}
