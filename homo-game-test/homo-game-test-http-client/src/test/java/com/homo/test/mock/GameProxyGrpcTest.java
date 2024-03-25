package com.homo.test.mock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.homo.game.proxy.enums.ProxyKey;
import com.homo.game.proxy.proxy.facade.ClientJsonRouterMsg;
import io.homo.proto.client.*;
import io.homo.proto.entity.EntityRequest;
import io.homo.proto.entity.EntityResponse;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@WebFluxTest
@AutoConfigureWebTestClient
@SpringBootTest(classes = HttpClientApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GameProxyGrpcTest {
    @Autowired
    private WebTestClient webTestClient;

    @BeforeTestClass
    public void setUp() {
        log.info("WebFluxControllerTest setUp!");
    }

    /**
     * @ServiceExport(tagName = "entity-test-service:11555",driverType = RpcType.grpc,isMainServer = true,isStateful = true)
     * public interface IUserService {
     * <p>
     * Homo<TestRsp> test(Integer podId, ParameterMsg parameterMsg, TestReq testReq);
     * }
     * @EntityType(type = "user")
     * @StorageTime(10000)
     * @CacheTime(10000) public interface IUerEntity {
     * Homo<TestRsp> test(TestReq testReq);
     * }
     * }
     */
    public static String GAME_PROXY_URL = "http://homo-game-proxy:31506/";
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
        ClientRouterMsg clientRouterMsg = ClientRouterMsg.newBuilder()
                .setSrcService("entity-test-service")
                .setMsgId("testProto")
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
    public void entityCallTest() throws InterruptedException, NoSuchAlgorithmException, InvalidProtocolBufferException {
        TestReq testReq = TestReq.newBuilder().setToken("123").setSign("2313").build();
        EntityRequest entityRequest = EntityRequest.newBuilder().setType("user").setFunName("test").setSrcName("client").addContent(testReq.toByteString()).build();
        ClientRouterMsg clientRouterMsg = ClientRouterMsg.newBuilder()
                .setSrcService("entity-test-service")
                .setEntityType("user")
                .setMsgId("entityCall")
                .addMsgContent(entityRequest.toByteString())
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
        EntityResponse response = EntityResponse.parseFrom(responseBody.getMsgContent().toByteArray());
        TestRsp testRsp = TestRsp.parseFrom(response.getContent(0));
        log.info("entityServiceTest responseBody {} response {} testRsp {}", responseBody, response, testRsp);
    }

    @Test
    public void jsonCallTest() throws InterruptedException, NoSuchAlgorithmException, InvalidProtocolBufferException {
        JSONObject param = new JSONObject();
        param.put("param1", 1);
        param.put("param2", "test");
        ClientJsonRouterMsg clientRouterMsg = ClientJsonRouterMsg.builder()
                .serviceName("entity-test-service:11555")
                .msgId("testJson")
                .msgContent(param.toJSONString())
                .podIndex(0)
                .build();
        String responseBody = webTestClient.post()
                .uri(GAME_PROXY_URL + "clientJsonMsgProxy")
                .headers(httpHeaders -> {
                    for (Map.Entry<String, String> entry : frameHeader.entrySet()) {
                        httpHeaders.set(entry.getKey(), entry.getValue());
                    }
                    httpHeaders.set("Content-Type", "application/json");
                })
                .body(BodyInserters.fromValue(JSON.toJSONBytes(clientRouterMsg)))
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        log.info("entityServiceTest responseBody {}", responseBody);
    }
}
