package com.homo.test.mock;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.homo.game.proxy.enums.ProxyKey;
import io.homo.proto.client.HttpTestReq;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@WebFluxTest
@AutoConfigureWebTestClient
@SpringBootTest(classes = HttpClientApplication.class,webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CommonProxyHttpForwardTest {
    @Autowired
    private WebTestClient webTestClient;

    @BeforeTestClass
    public void setUp() {
        log.info("WebFluxControllerTest setUp!");
    }

    /**
     * @ServiceExport(tagName = "http-test-service:33333", isStateful = false, driverType = RpcType.http, isMainServer = true)
     * public interface IHttpTestService {
     * Homo<String> jsonGetTest1(JSONObject params, JSONObject header);
     * <p>
     * Homo<String> jsonGetTest2(String params, JSONObject header);
     * <p>
     * Homo<String> jsonGetTest3(String params, Integer intParam, JSONObject header);
     * <p>
     * Homo<String> jsonPostTest1(JSONObject params, JSONObject header);
     * <p>
     * Homo<String> jsonPostTest2(String params, JSONObject header);
     * <p>
     * Homo<String> jsonPostTest3(String params, Integer intParam, JSONObject header);
     * <p>
     * Homo<String> jsonPostTest4(JSONObject param1, JSONObject param2,JSONObject header);
     * <p>
     * Homo<HttpTestRsp> protoPostTest(HttpTestReq req, ClientRouterHeader header);
     * }
     */
    public static String COMMON_PROXY_URL = "http://homo-common-proxy:33306/httpForward/";
    public static HashMap<String, String> frameHeader;
    public static String token = "token123";
    public static String userId = "20001_123";
    static {
        frameHeader = new HashMap<>();
        frameHeader.put(ProxyKey.X_HOMO_APP_ID, "1");
        frameHeader.put(ProxyKey.X_HOMO_TOKEN, token);
        frameHeader.put(ProxyKey.X_HOMO_SIGNATURE, "3");
        frameHeader.put(ProxyKey.X_HOMO_USER_ID, userId);
        frameHeader.put(ProxyKey.X_HOMO_RESPONSE, "5");
        frameHeader.put(ProxyKey.X_HOMO_RESPONSE_TIME, "10000");
        frameHeader.put(ProxyKey.X_HOMO_CHANNEL_ID, "7");
    }

    @BeforeAll
    public void login(){
        JSONObject body = new JSONObject();
        body.put("userId", userId);
        String responseBody = webTestClient.post()
                .uri(COMMON_PROXY_URL + "login-service-http/login")
//                .accept(MediaType.APPLICATION_JSON)
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
        log.info("token responseBody {} ", JSONObject.parseObject(responseBody));
    }

    @Test
    public void jsonGetTest1() throws InterruptedException {
        String responseBody = webTestClient.get()
                .uri(COMMON_PROXY_URL + "http-test-service/jsonGetTest1?name=test")
                .headers(httpHeaders -> {
                    for (Map.Entry<String, String> entry : frameHeader.entrySet()) {
                        httpHeaders.set(entry.getKey(), entry.getValue());
                    }
                })
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        log.info("jsonGetTest1 responseBody {} ", JSONObject.parseObject(responseBody));
    }

    @Test
    public void jsonGetTest2() throws InterruptedException {
        String responseBody = webTestClient.get()
                .uri(COMMON_PROXY_URL + "http-test-service/jsonGetTest2?name=test")
                .headers(httpHeaders -> {
                    for (Map.Entry<String, String> entry : frameHeader.entrySet()) {
                        httpHeaders.set(entry.getKey(), entry.getValue());
                    }
                })
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        log.info("jsonGetTest2 responseBody {} ", JSONObject.parseObject(responseBody));
    }

    @Test
    public void jsonPostTest1() throws InterruptedException {
        JSONObject body = new JSONObject();
        body.put("bodyKey", "bodyValue");
        String responseBody = webTestClient.post()
                .uri(COMMON_PROXY_URL + "http-test-service/jsonPostTest1?name=test")
//                .accept(MediaType.APPLICATION_JSON)
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
        log.info("jsonPostTest1 responseBody {} ", JSONObject.parseObject(responseBody));
    }

    @Test
    public void jsonPostTest2() throws InterruptedException {
        JSONObject body = new JSONObject();
        body.put("bodyKey", "bodyValue");
        String responseBody = webTestClient.post()
                .uri(COMMON_PROXY_URL + "http-test-service/jsonPostTest2?name=test")
//                .accept(MediaType.APPLICATION_JSON)
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
        log.info("jsonPostTest2 responseBody {} ", JSONObject.parseObject(responseBody));
    }

    @Test
    public void jsonPostTest3() throws InterruptedException {
        String responseBody = webTestClient.post()
                .uri(COMMON_PROXY_URL + "http-test-service/jsonPostTest3?name=test")
//                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> {
                    for (Map.Entry<String, String> entry : frameHeader.entrySet()) {
                        httpHeaders.set(entry.getKey(), entry.getValue());
                    }
                })
                .body(BodyInserters.fromValue("bodyStr"))
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        log.info("jsonPostTest3 responseBody {} ", JSONObject.parseObject(responseBody));
    }

    @Test
    public void jsonPostTest4() throws InterruptedException {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("bodyKey1", "bodyValue1");
        JSONObject jsonObject2 = new JSONObject();
        jsonObject2.put("bodyKey2", "bodyValue2");
        jsonArray.add(jsonObject1);
        jsonArray.add(jsonObject2);
        String responseBody = webTestClient.post()
                .uri(COMMON_PROXY_URL + "http-test-service/jsonPostTest4?name=test")
//                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> {
                    for (Map.Entry<String, String> entry : frameHeader.entrySet()) {
                        httpHeaders.set(entry.getKey(), entry.getValue());
                    }
                })
                .body(BodyInserters.fromValue(jsonArray.toJSONString()))
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        log.info("jsonPostTest4 responseBody {} ", JSONObject.parseObject(responseBody));
    }

    @Test
    public void jsonPostTest5() throws InterruptedException {
        JSONObject body = new JSONObject();
        body.put("bodyKey", "bodyValue");
        String responseBody = webTestClient.post()
                .uri(COMMON_PROXY_URL + "http-test-service/jsonPostTest5?name=test")
//                .accept(MediaType.APPLICATION_JSON)
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
        log.info("jsonPostTest5 responseBody {} ", JSONObject.parseObject(responseBody));
    }

    @Test
    public void protoPostTest1() throws InterruptedException {
        HttpTestReq httpTestReq = HttpTestReq.newBuilder().setSign("sign").setChannelId("channelId").build();
        String responseBody = webTestClient.post()
                .uri(COMMON_PROXY_URL + "http-test-service/protoPostTest1?name=test")
                .headers(httpHeaders -> {
                    for (Map.Entry<String, String> entry : frameHeader.entrySet()) {
                        httpHeaders.set(entry.getKey(), entry.getValue());
                    }
                    httpHeaders.set("Content-Type", "application/x-protobuf");
                })
                .body(BodyInserters.fromValue(httpTestReq.toByteArray()))
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        log.info("protoPostTest1 responseBody {} ", JSONObject.parseObject(responseBody));
    }

    @Test
    public void protoPostTest2() throws InterruptedException {
        HttpTestReq httpTestReq = HttpTestReq.newBuilder().setSign("sign").setChannelId("channelId").build();
        String responseBody = webTestClient.post()
                .uri(COMMON_PROXY_URL + "http-test-service/protoPostTest2?name=test")
                .headers(httpHeaders -> {
                    for (Map.Entry<String, String> entry : frameHeader.entrySet()) {
                        httpHeaders.set(entry.getKey(), entry.getValue());
                    }
                    httpHeaders.set("Content-Type", "application/x-protobuf");
                })
                .body(BodyInserters.fromValue(httpTestReq.toByteArray()))
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        log.info("protoPostTest2 responseBody {} ", JSONObject.parseObject(responseBody));
    }
}
