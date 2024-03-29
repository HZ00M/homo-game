package com.homo.test.mock;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.homo.game.proxy.enums.ProxyKey;
import io.homo.proto.client.HttpTestReq;
import io.homo.proto.client.Msg;
import io.homo.proto.rpc.Req;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
@SpringBootTest(classes = HttpClientApplication.class,webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CommonProxyGrpcTest {
    @Autowired
    private WebTestClient webTestClient;

    @BeforeTestClass
    public void setUp() {
        log.info("WebFluxControllerTest setUp!");
    }

    /**
     * @ServiceExport(tagName = "rpc-test-service:33334", isStateful = false, driverType = RpcType.grpc, isMainServer = false)
     * public interface IRpcTestService {
     *
     *     Homo<String> jsonPostTest1(JSONObject params, JSONObject header);
     *
     *     Homo<HttpTestRsp> protoPostTest1(HttpTestReq req);
     * }
     */
    public static String COMMON_PROXY_URL = "http://homo-common-proxy:33306/";
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
    public void clientJsonMsgCheckToken() throws InterruptedException {
        JSONObject msgContent  = new JSONObject();
        msgContent.put("msg","msgBody");
        JSONObject body = new JSONObject();
        body.put("srcService", "rpc-test-service");
        body.put("msgId", "jsonPostTest1");
        body.put("msgContent",msgContent);
        String responseBody = webTestClient.post()
                .uri(COMMON_PROXY_URL + "clientJsonMsgCheckToken")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> {
                    for (Map.Entry<String, String> entry : frameHeader.entrySet()) {
                        httpHeaders.set(entry.getKey(), entry.getValue());
                    }
                })
                .body(BodyInserters.fromValue(body.toJSONString()))
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        log.info("clientJsonMsgCheckToken responseBody {} ", responseBody);
    }

    @Test
    public void clientJsonMsgCheckSign() throws InterruptedException {
        JSONObject msgContent  = new JSONObject();
        msgContent.put("msg","msgBody");
        JSONObject body = new JSONObject();
        body.put("srcService", "rpc-test-service");
        body.put("msgId", "jsonPostTest1");
        body.put("msgContent",msgContent);
        String responseBody = webTestClient.post()
                .uri(COMMON_PROXY_URL + "clientJsonMsgCheckSign")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> {
                    for (Map.Entry<String, String> entry : frameHeader.entrySet()) {
                        httpHeaders.set(entry.getKey(), entry.getValue());
                    }
                })
                .body(BodyInserters.fromValue(body.toJSONString()))
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        log.info("clientJsonMsgCheckSign responseBody {} ", responseBody);
    }


    @Test
    public void clientPbMsgCheckToken() throws InterruptedException, InvalidProtocolBufferException {
        HttpTestReq httpTestReq = HttpTestReq.newBuilder().setSign("sign").setChannelId("channelId").build();
        Req req = Req.newBuilder().setSrcService("rpc-test-service").setMsgId("protoPostTest1").addMsgContent(httpTestReq.toByteString()).build();
        Msg responseBody = webTestClient.post()
                .uri(COMMON_PROXY_URL + "clientPbMsgCheckToken")
                .headers(httpHeaders -> {
                    for (Map.Entry<String, String> entry : frameHeader.entrySet()) {
                        httpHeaders.set(entry.getKey(), entry.getValue());
                    }
                    httpHeaders.set("Content-Type", "application/x-protobuf");
                })
                .body(BodyInserters.fromValue(req.toByteArray()))
                .exchange()
                .expectBody(Msg.class)
                .returnResult()
                .getResponseBody();
        log.info("clientPbMsgCheckToken responseBody {} ", responseBody);
    }

    @Test
    public void clientPbMsgCheckSign() throws InterruptedException, NoSuchAlgorithmException {
        HttpTestReq httpTestReq = HttpTestReq.newBuilder().setSign("sign").setChannelId("channelId").build();
        Req req = Req.newBuilder().setSrcService("rpc-test-service").setMsgId("protoPostTest1").addMsgContent(httpTestReq.toByteString()).build();
        String clientSign = clientSign("1", "protoPostTest1", req.toByteArray());
        Msg responseBody = webTestClient.post()
                .uri(COMMON_PROXY_URL + "clientPbMsgCheckSign")
                .headers(httpHeaders -> {
                    for (Map.Entry<String, String> entry : frameHeader.entrySet()) {
                        httpHeaders.set(entry.getKey(), entry.getValue());
                    }
                    httpHeaders.set("Content-Type", "application/x-protobuf");
                    httpHeaders.set(ProxyKey.X_HOMO_SIGNATURE,clientSign);
                })
                .body(BodyInserters.fromValue(req.toByteArray()))
                .exchange()
                .expectBody(Msg.class)
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
