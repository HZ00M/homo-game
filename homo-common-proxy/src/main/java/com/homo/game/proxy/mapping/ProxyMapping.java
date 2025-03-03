package com.homo.game.proxy.mapping;

import brave.Span;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.homo.game.proxy.enums.ProxyKey;
import com.homo.game.proxy.dto.HeaderParam;
import com.homo.game.proxy.dto.ProxyParam;
import com.homo.core.rpc.http.HttpServer;
import com.homo.core.rpc.http.mapping.AbstractHttpMapping;
import com.homo.core.utils.trace.ZipkinUtil;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.rpc.Req;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class ProxyMapping extends AbstractHttpMapping {
    /**
     * http请求映射   请求为header+body,转换成json字符串 list{headerJson{},requestJson{method,url,msg[form-data|body]}}
     *
     * @param exchange
     * @return
     */
    @RequestMapping(value = "/httpForward/**")
    public Mono<Void> httpForward(ServerWebExchange exchange) {
        return callHttpForward("httpForward", exchange);
    }

    /**
     * http映射   请求为header+body,转换成json字符串 list{headerJson{},requestJson{method,url,msg[form-data|body]}}
     *
     * @param exchange
     * @return
     */
    @RequestMapping(value = "/innerHttpForward/**")
    public Mono<Void> innerHttpForward(ServerWebExchange exchange) {
        return callHttpForward("innerHttpForward", exchange);
    }

    /**
     * 客户端json请求映射   请求为header(框架参数X-HOMO-XXX)+body(ProxyParam),转换成json字符串 list{HeaderParam,ProxyParam}
     *
     * @param exchange
     * @return
     */
    @RequestMapping(value = {"/clientJsonMsgCheckToken", "/clientJsonMsgCheckSign"}, consumes = "application/json")
    public Mono<Void> clientGrpcJsonMsg(ServerWebExchange exchange) {
        String msgId = exchange.getRequest().getURI().getPath().split("/")[1];
        return callJsonForward(msgId, exchange);
    }

    /**
     * 客户端pb请求映射   请求为header(框架参数X-HOMO-XXX)+body(Req),转换成ClientRouterMsg
     *
     * @param exchange
     * @return
     */
    @RequestMapping(value = {"/clientPbMsgCheckToken", "/clientPbMsgCheckSign"}, consumes = "application/x-protobuf")
    public Mono<Void> clientGrpcProtoMsg(ServerWebExchange exchange) {
        String msgId = exchange.getRequest().getURI().getPath().split("/")[1];
        return callPbForward(msgId, exchange);
    }

    public Mono<Void> callHttpForward(String msgId, ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        int port = exportPort(request);
        HttpMethod method = request.getMethod();
        String url = request.getURI().getPath();
        HttpHeaders headers = request.getHeaders();
        Flux<DataBuffer> body = request.getBody();
        if (method != null && method.equals(HttpMethod.GET)) {
            String msg = buildStringMsg(request, null);
            return forwardProxy(msgId, msg, port, response);
        } else if (method != null && method.equals(HttpMethod.POST)) {
            return DataBufferUtils.join(body).flatMap(dataBuffer -> {
                try {
                    checkDataBufferSize(dataBuffer);
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    String msg = buildStringMsg(request, new String(bytes));
                    return forwardProxy(msgId, msg, port, response);
                } catch (Exception e) {
                    return Mono.error(e);
                }
            });
        } else {
            throw new RuntimeException("不支持的请求方法");
        }
    }

    public Mono<Void> callJsonForward(String msgId, ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        int port = exportPort(request);
        HttpMethod method = request.getMethod();
        String url = request.getURI().getPath();
        HttpHeaders headers = request.getHeaders();
        Flux<DataBuffer> body = request.getBody();
        return DataBufferUtils.join(body).flatMap(dataBuffer -> {
            try {
                checkDataBufferSize(dataBuffer);
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                String msg = buildJsonMsg(request.getHeaders(), new String(bytes));
                return forwardProxy(msgId, msg, port, response);
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }

    public Mono<Void> callPbForward(String msgId, ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        int port = exportPort(request);
        HttpMethod method = request.getMethod();
        String url = request.getURI().getPath();
        HttpHeaders headers = request.getHeaders();
        Flux<DataBuffer> body = request.getBody();
        return DataBufferUtils.join(body).flatMap(dataBuffer -> {
            try {
                checkDataBufferSize(dataBuffer);
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                byte[][] msg = buildPbMsg(request.getHeaders(), bytes);

                return routerProxy(msgId, msg, port, response);
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }

    public static byte[][] buildPbMsg(HttpHeaders headers, byte[] proxyParam) throws InvalidProtocolBufferException {
        String appId = headers.getFirst(ProxyKey.X_HOMO_APP_ID);
        String token = headers.getFirst(ProxyKey.X_HOMO_TOKEN);
        String userId = headers.getFirst(ProxyKey.X_HOMO_USER_ID);
        String channel = headers.getFirst(ProxyKey.X_HOMO_CHANNEL_ID);
        String sign = headers.getFirst(ProxyKey.X_HOMO_SIGNATURE);
        Req req = Req.parseFrom(proxyParam);
        ClientRouterMsg.Builder clientRouterBuilder = ClientRouterMsg.newBuilder()
                .setAppId(appId)
                .setToken(token)
                .setUserId(userId)
                .setChannelId(channel)
                .setSign(sign)
                .setSrcService(req.getSrcService())
                .setMsgId(req.getMsgId());
        List<ByteString> msgContentList = req.getMsgContentList();
        for (ByteString bytes : msgContentList) {
            clientRouterBuilder.addMsgContent(bytes);
        }
        ClientRouterMsg clientRouterMsg = clientRouterBuilder.build();
        byte[][] msg = new byte[1][];
        msg[0] = clientRouterMsg.toByteArray();
        return msg;
    }

    public static String buildJsonMsg(HttpHeaders headers, String proxyParamString) {
        //参数格式 (reqJson,headerJson)
        String appId = headers.getFirst(ProxyKey.X_HOMO_APP_ID);
        String token = headers.getFirst(ProxyKey.X_HOMO_TOKEN);
        String userId = headers.getFirst(ProxyKey.X_HOMO_USER_ID);
        String channel = headers.getFirst(ProxyKey.X_HOMO_CHANNEL_ID);
        String sign = headers.getFirst(ProxyKey.X_HOMO_SIGNATURE);
        ProxyParam proxyParam = JSON.parseObject(proxyParamString, ProxyParam.class);
        HeaderParam headerParam = HeaderParam.builder()
                .appId(appId)
                .channelId(channel)
                .sign(sign)
                .token(token)
                .userId(userId)
                .body(proxyParamString)
                .build();
        List<Object> list = new ArrayList<>();
        list.add(proxyParam);
        list.add(headerParam);
        return JSON.toJSONString(list);
    }

    public static String buildStringMsg(ServerHttpRequest request, String msg) {
        //参数格式(msgJson,headerJson)
        HttpMethod method = request.getMethod();
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        List<JSONObject> list = new ArrayList<>();
        JSONObject methodAndUrlJson = new JSONObject();
        methodAndUrlJson.put(ProxyKey.METHOD_KEY, method.toString());
        methodAndUrlJson.put(ProxyKey.URL_KEY, request.getURI().getPath());
        String queryParamsJson = JSON.toJSONString(queryParams.toSingleValueMap());
        if (method.equals(HttpMethod.POST)) {
            methodAndUrlJson.put(ProxyKey.MSG_KEY, msg);
        } else {
            methodAndUrlJson.put(ProxyKey.MSG_KEY, queryParamsJson);
        }
        list.add(methodAndUrlJson);
        JSONObject headerJson = new JSONObject();
        HttpHeaders headers = request.getHeaders();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            headerJson.put(entry.getKey(), entry.getValue().get(0));
        }
        list.add(headerJson);
        return JSON.toJSONString(list);
    }

    private Mono<Void> forwardProxy(String msgId, String msg, int port, ServerHttpResponse response) {
        HttpServer httpServer = routerHttpServerMap.get(port);
        return response.writeAndFlushWith(Mono.create(sink -> {
            httpServer.onJsonCall(msgId, msg, response)
                    .subscribe(ret -> {
                        sink.success(Mono.just(ret));
                    }, throwable -> {
                        sink.error(throwable);
                    }, () -> {

                    });
        }));
    }

    private Mono<Void> routerProxy(String msgId, byte[][] msg, int port, ServerHttpResponse response) {
        HttpServer httpServer = routerHttpServerMap.get(port);
        Span span = ZipkinUtil.currentSpan();
        log.info("routerProxy start msgId {} port {} ", msgId, port);
        return response.writeAndFlushWith(Mono.create(sink -> {
            httpServer.onBytesCall(msgId, msg, response)
                    .subscribe(ret -> {
                        span.tag("doHttpForward", "success");
                        log.info("routerProxy success msgId {} port {} ret {}", msgId, port, ret);
                        sink.success(Mono.just(ret));
                    }, throwable -> {
                        span.error(throwable).tag("doHttpForward", "error");
                        log.info("routerProxy error msgId {} port {} throwable {}", msgId, port, throwable);
                        sink.error(throwable);
                    }, () -> {
                        span.tag("doHttpForward Complete", "complete");
                    });
        }));
    }
}
