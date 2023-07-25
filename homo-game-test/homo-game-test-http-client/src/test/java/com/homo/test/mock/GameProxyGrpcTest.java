package com.homo.test.mock;

import com.google.protobuf.InvalidProtocolBufferException;
import com.homo.game.proxy.enums.ProxyKey;
import io.homo.proto.client.*;
import io.homo.proto.rpc.Req;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@WebFluxTest
@AutoConfigureWebTestClient
@SpringBootTest(classes = HttpClientApplication.class,webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GameProxyGrpcTest {
    @Autowired
    private WebTestClient webTestClient;

    @BeforeTestClass
    public void setUp() {
        log.info("WebFluxControllerTest setUp!");
    }

    /**
     *
     @ServiceExport(tagName = "entity-test-service:11555",driverType = RpcType.grpc,isMainServer = true,isStateful = true)
     public interface IUserService {

     Homo<TestRsp> test(Integer podId, ParameterMsg parameterMsg, TestReq testReq);
     }
     *
     @EntityType(type = "user")
     @StorageTime(10000)
     @CacheTime(10000)
     public interface IUerEntity {
     Homo<TestRsp> test(TestReq testReq);
     }
     * }
     */
    public static String GAME_PROXY_URL = "http://http-client-proxy:31506/";
    public static HashMap<String, String> frameHeader;

    static {
        frameHeader = new HashMap<>();
        frameHeader.put(ProxyKey.X_HOMO_APP_ID, "1");
        frameHeader.put(ProxyKey.X_HOMO_TOKEN, "2");
        frameHeader.put(ProxyKey.X_HOMO_SIGNATURE, "065bc86090018d47ec4560ed3ae0b41b");
        frameHeader.put(ProxyKey.X_HOMO_USER_ID, "5");
        frameHeader.put(ProxyKey.X_HOMO_RESPONSE, "5");
        frameHeader.put(ProxyKey.X_HOMO_RESPONSE_TIME, "10000");
        frameHeader.put(ProxyKey.X_HOMO_CHANNEL_ID, "7");
    }



    @Test
    public void normalServiceTest() throws InterruptedException, InvalidProtocolBufferException {
        TestReq httpTestReq = TestReq.newBuilder().setSign("sign").setChannelId("channelId").build();
        ClientRouterMsg clientRouterMsg =  ClientRouterMsg.newBuilder()
                .setSrcService("entity-test-service")
                .setMsgId("test")
                .addMsgContent(httpTestReq.toByteString())
                .setSign("52f633ec818319aac1180cbcbece882c")
                .setToken("token")
                .setUserId("123")
                .setChannelId("channel")
                .setAppId("1")
                .build();
        Msg responseBody = webTestClient.post()
                .uri(GAME_PROXY_URL + "clientMsgProxy")
                .headers(httpHeaders -> {
                    for (Map.Entry<String, String> entry : frameHeader.entrySet()) {
                        httpHeaders.set(entry.getKey(), entry.getValue());
                    }
                    httpHeaders.set("Content-Type", "application/x-protobuf");
                })
                .body(BodyInserters.fromValue(clientRouterMsg.toByteArray()))
                .exchange()
                .expectBody(Msg.class)
                .returnResult()
                .getResponseBody();
        log.info("entityServiceTest responseBody {} ", responseBody);
    }

    @Test
    public void clientPbMsgCheckSign() throws InterruptedException, NoSuchAlgorithmException {
        HttpTestReq httpTestReq = HttpTestReq.newBuilder().setSign("sign").setChannelId("channelId").build();
        Req req = Req.newBuilder().setSrcService("rpc-test-service").setMsgId("protoPostTest1").addMsgContent(httpTestReq.toByteString()).build();
        String clientSign = clientSign("1", "protoPostTest1", req.toByteArray());
        PbResponseMsg responseBody = webTestClient.post()
                .uri(GAME_PROXY_URL + "clientPbMsgCheckSign")
                .headers(httpHeaders -> {
                    for (Map.Entry<String, String> entry : frameHeader.entrySet()) {
                        httpHeaders.set(entry.getKey(), entry.getValue());
                    }
                    httpHeaders.set("Content-Type", "application/x-protobuf");
                    httpHeaders.set(ProxyKey.X_HOMO_SIGNATURE,clientSign);
                })
                .body(BodyInserters.fromValue(req.toByteArray()))
                .exchange()
                .expectBody(PbResponseMsg.class)
                .returnResult()
                .getResponseBody();
        log.info("clientPbMsgCheckSign responseBody {} ", responseBody);
    }

    private String clientSign(String appId, String msgId, byte[] msgBody) throws NoSuchAlgorithmException {
        String secret = "123";
        String contentMd5 = TestSignatureUtil.contentMd5(msgBody);
        Map<String, String> param = new HashMap<>(4);
        param.put("appId", appId);
        param.put("messageId", msgId);
        param.put("content-md5", contentMd5);
        return TestSignatureUtil.sign(param, secret);
    }
}
