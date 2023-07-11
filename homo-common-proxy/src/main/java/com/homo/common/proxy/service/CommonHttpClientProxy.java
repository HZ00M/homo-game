package com.homo.common.proxy.service;

import brave.Span;
import com.alibaba.csp.sentinel.AsyncEntry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.apollo.ApolloDataSource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.protobuf.ByteString;
import com.homo.common.proxy.config.CommonProxyProperties;
import com.homo.common.proxy.config.ProxyKey;
import com.homo.common.proxy.dto.HeaderParam;
import com.homo.common.proxy.dto.HttpParam;
import com.homo.common.proxy.dto.ProxyParam;
import com.homo.common.proxy.dto.ResponseMsg;
import com.homo.common.proxy.enums.HomoCommonProxyError;
import com.homo.common.proxy.facade.ICommonHttpClientProxy;
import com.homo.common.proxy.util.ProxyCheckParamUtils;
import com.homo.common.proxy.util.ProxySignatureUtil;
import com.homo.core.facade.rpc.RpcAgentClient;
import com.homo.core.root.storage.ObjStorage;
import com.homo.core.rpc.base.serial.ByteRpcContent;
import com.homo.core.rpc.base.serial.JsonRpcContent;
import com.homo.core.rpc.base.service.BaseService;
import com.homo.core.rpc.client.RpcClientMgr;
import com.homo.core.utils.http.HttpCallerFactory;
import com.homo.core.utils.module.ServiceModule;
import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.secret.EncryptUtils;
import com.homo.core.utils.trace.ZipkinUtil;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.PbResponseMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CommonHttpClientProxy extends BaseService implements ICommonHttpClientProxy, ServiceModule {
    private static final String RESOURCE_HTTP_FORWARD = "httpForward";
    @Autowired
    private CommonProxyProperties commonProxyProperties;
    @Autowired
    private ObjStorage storage;
    @Autowired
//    @Lazy
    private HttpCallerFactory httpCallerFactory;
    @Autowired
    private RpcClientMgr rpcClientMgr;

    @Override
    public void init() {
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new ApolloDataSource<>(commonProxyProperties.getDatasourceNamespace(), commonProxyProperties.getFlowRuleKey(),
                "[]", new Converter<String, List<FlowRule>>() {
            @Override
            public List<FlowRule> convert(String source) {
                List<FlowRule> flowRules = JSONObject.parseObject(source, new TypeReference<List<FlowRule>>() {
                });
                log.info("flowRules is change flowRules_{}", flowRules);
                return flowRules;
            }
        });
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
    }

    @Override
    public Homo<String> httpForward(JSONObject headerJson, JSONObject requestJson) {
        try {
            log.info("httpForward begin headerJson {} requestJson {}", headerJson, requestJson);
            AsyncEntry entry;
            String method = requestJson.getString(ProxyKey.METHOD_KEY);
            String url = requestJson.getString(ProxyKey.URL_KEY);
            String msg = requestJson.getString(ProxyKey.MSG_KEY);
            String[] routes = url.split("/");
            String forwardKey = routes[2];
            if (commonProxyProperties.getFlowControlKeys().contains(forwardKey)) {
                entry = SphU.asyncEntry(RESOURCE_HTTP_FORWARD);
            } else {
                entry = null;
            }
            Homo<Boolean> canForwardPromise;
            if (commonProxyProperties.getIgnoreCheckTokenKeys().contains(forwardKey)) {
                canForwardPromise = Homo.result(true);
            } else {
                String appId = headerJson.getString(ProxyKey.X_HOMO_APP_ID);
                String token = headerJson.getString(ProxyKey.X_HOMO_TOKEN);
                String userId = headerJson.getString(ProxyKey.X_HOMO_USER_ID);
                String channelId = headerJson.getString(ProxyKey.X_HOMO_CHANNEL_ID);
                canForwardPromise = checkToken(appId, channelId, userId, token);
            }
            return canForwardPromise
                    .nextDo(pass -> {
                        if (!pass) {
                            log.error("check token error headersJson_{} requestJson_{}", headerJson, requestJson);
                            ResponseMsg response = ResponseMsg.builder().code(HomoCommonProxyError.token_error.getCode()).msg(HomoCommonProxyError.token_error.message()).build();
                            JSONObject responseHeader = new JSONObject();
                            responseHeader.put(ProxyKey.X_HOMO_RESPONSE, "check token error");
                            HttpParam httpParam = HttpParam.builder().headers(responseHeader.toJSONString()).code(HttpStatus.OK.value()).result(JSON.toJSONString(response)).build();
                            return Homo.result(JSONObject.toJSONString(httpParam));
                        }
                        String forwardUrl = commonProxyProperties.getForwardUrlMap().get(forwardKey);
                        if (forwardUrl == null) {
                            log.error("forwardUrl == null requestJson_{}", requestJson);
                            ResponseMsg response = ResponseMsg.builder().code(HomoCommonProxyError.no_forward_url.getCode()).msg(HomoCommonProxyError.no_forward_url.msgFormat(forwardKey)).build();
                            HttpParam httpParam = HttpParam.builder().code(HttpStatus.OK.value()).result(JSON.toJSONString(response)).build();
                            return Homo.result(JSONObject.toJSONString(httpParam));
                        }
                        StringBuilder forwardUrlBuilder = new StringBuilder(forwardUrl);
                        //因为第一个是httpForward，第二个是反向代理的url的forwardKey，所以从第三个开始是业务需要转发的url
                        for (int i = 3; i < routes.length; i++) {
                            forwardUrlBuilder.append("/").append(routes[i]);
                        }
                        if (HttpMethod.GET.matches(method)) {
                            //get请求拼接参数
                            forwardUrlBuilder.append("?");
                            JSONObject param = JSON.parseObject(msg);
                            for (Map.Entry<String, Object> paramEntry : param.entrySet()) {
                                forwardUrlBuilder.append(paramEntry.getKey()).append("=").append(paramEntry.getValue()).append("&");
                            }
                        }
                        String forwardUrlWithParam = forwardUrlBuilder.toString();
                        Span span = ZipkinUtil.getTracing().tracer().currentSpan();
                        if (span != null) {
                            span.name("httpForward").tag("url", forwardUrl);
                        }
                        return httpCall(entry, forwardUrl, forwardUrlWithParam, method, msg, headerJson, requestJson);
                    });
        } catch (BlockException e) {
            log.error("httpForward over limit count headerJson {} requestJson {}", headerJson, requestJson);
            ResponseMsg responseMsg = ResponseMsg.builder()
                    .code(HomoCommonProxyError.flow_limit_error.getCode())
                    .msg(HomoCommonProxyError.flow_limit_error.msgFormat(e.getMessage()))
                    .build();
            HttpParam httpParam = HttpParam.builder().code(HttpStatus.OK.value()).result(JSONObject.toJSONString(responseMsg)).build();
            return Homo.result(JSONObject.toJSONString(httpParam));
        } catch (Exception e) {
            log.error("httpForward system error {} {}", headerJson, requestJson, e);
            ResponseMsg responseMsg = ResponseMsg.builder()
                    .code(HomoCommonProxyError.common_system_error.getCode())
                    .msg(HomoCommonProxyError.common_system_error.msgFormat(e.getMessage()))
                    .build();
            HttpParam httpParam = HttpParam.builder().code(HttpStatus.OK.value()).result(JSONObject.toJSONString(responseMsg)).build();
            return Homo.result(JSONObject.toJSONString(httpParam));
        }
    }

    public Homo<String> httpCall(AsyncEntry asyncEntry, String host, String method, String msg, String url, JSONObject headersJson, JSONObject requestJson) {
        return Homo.warp(() -> {
            String responseTimeStr = headersJson.getString(ProxyKey.X_HOMO_RESPONSE_TIME);
            long responseTime = responseTimeStr == null ? 10000 : Long.parseLong(responseTimeStr);
            headersJson.remove("Host");
            Span span = ZipkinUtil.getTracing()
                    .tracer()
                    .currentSpan();
            HttpClient httpClient = httpCallerFactory.getHttpClientCache(host);
            Mono<HttpParam> httpParamMono = httpClient
                    .followRedirect(false)//不允许重定向
                    .headers(h -> {
                        for (Map.Entry<String, Object> entry : headersJson.entrySet()) {
                            h.set(entry.getKey(), entry.getValue());
                        }
                    })
                    .responseTimeout(Duration.ofMillis(responseTime))
                    .request(io.netty.handler.codec.http.HttpMethod.valueOf(method))
                    .uri(url)
                    .send(ByteBufFlux.fromString(Mono.just(msg)))
                    .responseSingle((httpClientResponse, byteBufMono) -> {
                        int code = httpClientResponse.status().code();
                        JSONObject respHeader = new JSONObject();
                        for (Map.Entry<String, String> responseHeader : httpClientResponse.responseHeaders()) {
                            respHeader.put(responseHeader.getKey(), responseHeader.getValue());
                        }

                        return Homo.warp(sink -> {
                            byteBufMono.asString()
                                    .subscribe(resp -> {
                                        HttpParam httpParam = HttpParam.builder().code(code).headers(respHeader.toJSONString()).result(resp).build();
                                        sink.success(httpParam);
                                    });
                        });
                    });
            return Homo.warp(() -> httpParamMono)
                    .switchToCurrentThread()
                    .nextDo(httpParam -> {
                        if (span != null) {
                            span.annotate("Http Finish")
                                    .tag("httpCode", httpParam.getCode() + "");
                        }
                        if (asyncEntry != null) {
                            asyncEntry.exit();
                        }
                        log.info("httpCall success code {} url {} body {}", httpParam.getCode(), url, msg);
                        return Homo.result(JSON.toJSONString(httpParam));
                    })
                    .onErrorContinue(throwable -> {
                        if (span != null) {
                            span.annotate("HttpCall error");
                        }
                        if (asyncEntry != null) {
                            asyncEntry.exit();
                        }
                        log.error("httpCall error headersJson {} requestJson {}", headersJson, requestJson, throwable);
                        return Homo.error(throwable);
                    });
        });
    }


    private Homo<Boolean> checkToken(String appId, String token, String userId, String channelId) {
        return storage.load(appId, channelId, "1", userId, "token", String.class)
                .nextDo(saveToken -> {
                    if (!token.equals(saveToken)) {
                        //todo 需要通过authUrl向认证服务器认证token, 这里简化逻辑，内部认证token
                        JSONObject payload = new JSONObject();
                        payload.put("id", userId);
                        payload.put("token", token);
                        String originSign = String.format("appId=%s&channelId=%s&id=%s&secret=%s", appId, channelId, userId, commonProxyProperties.getAppSecretKeyMap().get(appId));
                        String realSign = EncryptUtils.md5(originSign);
                        payload.put("sign", realSign);
                        Map<String, String> headers = new HashMap<>();
                        headers.put("appId", appId);
                        headers.put("channelId", channelId);
                        JSONObject authRet = authToken(headers, payload);
                        int errorCode = authRet.getIntValue("errorCode");
                        if (errorCode == 0) {
                            return storage.save(appId, channelId, "1", userId, "token", token).nextDo(ret -> Homo.result(true));
                        } else {
                            return Homo.result(false);
                        }
                    } else {
                        return Homo.result(true);
                    }
                });
    }


    private JSONObject authToken(Map<String, String> headers, JSONObject payload) {
        //todo 这里本需要远程验证，简化流程
        String sign = payload.getString("sign");
        String originSign = String.format("appId=%s&channelId=%s&id=%s&secret=%s", headers.get("appId"), headers.get("channelId"), payload.getString("id"), commonProxyProperties.getAppSecretKeyMap().get(headers.get("appId")));
        JSONObject result = new JSONObject();
        if (!sign.equals(EncryptUtils.md5(originSign))) {
            result.put("errorCode", 1);
        } else {
            result.put("errorCode", 0);
        }
        return result;
    }


    @Override
    public Homo<String> clientJsonMsgCheckToken(HeaderParam headerParam, ProxyParam proxyParam) {
        log.info("clientJsonMsgCheckToken call headerParam {} proxyParam {}", headerParam, proxyParam);
        String srcService = proxyParam.getSrcService();
        String msgId = proxyParam.getMsgId();
        String msgContent = proxyParam.getMsgContent();
        String userId = headerParam.getUserId();
        String token = headerParam.getToken();
        String appId = headerParam.getAppId();
        String channelId = headerParam.getChannelId();
        if (ProxyCheckParamUtils.checkIsNullOrEmpty(srcService, msgId, msgContent, userId, token, appId, channelId)) {
            log.error("clientJsonMsgCheckToken is error headerParam {} proxyParam {}", headerParam, proxyParam);
            ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonProxyError.param_miss.getCode()).msg(HomoCommonProxyError.param_miss.message()).build();
            return Homo.result(JSON.toJSONString(responseMsg));
        }

        return checkToken(appId, token, userId, channelId)
                .nextDo(checkPass -> {
                    if (!checkPass) {
                        log.error("clientJsonMsgCheckToken is error headerParam {} proxyParam {}", headerParam, proxyParam);
                        ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonProxyError.token_error.getCode()).msg(HomoCommonProxyError.token_error.message()).build();
                        return Homo.result(JSON.toJSONString(responseMsg));
                    }
                    JSONArray paramArray = JSON.parseArray(msgContent);
                    //把消息头添加到最后一个参数
                    paramArray.add(headerParam);
                    JsonRpcContent rpcContent = JsonRpcContent.builder().data(paramArray.toJSONString()).build();
                    String serverPort = commonProxyProperties.getServerPortMap().get(srcService);
                    String serviceName = srcService + ":" + serverPort;
                    RpcAgentClient agentClient = rpcClientMgr.getGrpcServerlessAgentClient(serviceName);
                    Homo<String> homo = agentClient.rpcCall(msgId, rpcContent)
                            .nextDo(ret -> {
                                String jsonRet = (String) ret;
                                log.info("proxyCall serviceName {} msgId {} rpcContent {} jsonRet {}", serviceName, msgId, rpcContent, jsonRet);
                                ResponseMsg responseMsg = ResponseMsg.builder()
                                        .code(HomoCommonProxyError.success.getCode())
                                        .msg(HomoCommonProxyError.success.message())
                                        .msgId(msgId)
                                        .msgContent(jsonRet)
                                        .build();
                                return Homo.result(JSON.toJSONString(responseMsg));
                            });
                    return homo;
                })
                .errorContinue(throwable -> {
                    log.error("clientJsonMsgCheckToken is error headerParam {} proxyParam {} e", headerParam, proxyParam, throwable);
                    ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonProxyError.common_system_error.getCode()).msg(HomoCommonProxyError.common_system_error.message()).build();
                    return Homo.result(JSON.toJSONString(responseMsg));
                });
    }

    @Override
    public Homo<byte[]> clientPbMsgCheckToken(ClientRouterMsg clientRouterMsg) {
        String srcService = clientRouterMsg.getSrcService();
        String msgId = clientRouterMsg.getMsgId();
        String userId = clientRouterMsg.getUserId();
        String token = clientRouterMsg.getToken();
        String appId = clientRouterMsg.getAppId();
        String channelId = clientRouterMsg.getChannelId();
        log.info("clientPbMsgCheckToken userId {} srcService {} msgId {} appId {} channelId {}",
                userId, srcService, msgId, appId, channelId);
        if (ProxyCheckParamUtils.checkIsNullOrEmpty(srcService, msgId, userId, token, appId, channelId)) {
            PbResponseMsg responseMsg = PbResponseMsg.newBuilder().setCode(HomoCommonProxyError.param_miss.getCode()).setMsg(HomoCommonProxyError.param_miss.message()).build();
            return Homo.result(responseMsg.toByteArray());
        }

        return checkToken(appId,token,userId,channelId)
                .nextDo(checkPass->{
                    if (!checkPass){
                        log.info("clientPbMsgCheckToken !checkPass userId {} srcService {} msgId {} appId {} channelId {}",
                                userId, srcService, msgId, appId, channelId);
                        PbResponseMsg responseMsg = PbResponseMsg.newBuilder().setCode(HomoCommonProxyError.token_error.getCode()).setMsg(HomoCommonProxyError.token_error.message()).build();
                    }
                    List<ByteString> msgContentList = clientRouterMsg.getMsgContentList();
                    byte[][] params = new byte[msgContentList.size()][];
                    for (ByteString bytes : msgContentList) {
                        params[0] = bytes.toByteArray();
                    }
                    String serverPort = commonProxyProperties.getServerPortMap().get(srcService);
                    String serviceName = srcService + ":" + serverPort;
                    ByteRpcContent rpcContent = ByteRpcContent.builder().data(params).build();
                    RpcAgentClient agentClient = rpcClientMgr.getGrpcServerlessAgentClient(serviceName);
                    Homo<byte[]> homo = agentClient.rpcCall(msgId, rpcContent)
                            .nextDo(ret -> {
                                byte[] byteRet = (byte[]) ret;
                                log.info("proxyCall serviceName {} msgId {} rpcContent {} ", serviceName, msgId, rpcContent);
                                PbResponseMsg responseMsg = PbResponseMsg.newBuilder()
                                        .setCode(HomoCommonProxyError.success.getCode())
                                        .setMsg(HomoCommonProxyError.success.message())
                                        .setMsgId(msgId)
                                        .setMsgContent(ByteString.copyFrom(byteRet))
                                        .build();
                                return Homo.result(JSON.toJSONString(responseMsg));
                            });
                    return homo;
                });
    }


    @Override
    public Homo<String> clientJsonMsgCheckSign(HeaderParam headerParam, ProxyParam proxyParam) {
        log.info("clientJsonMsgCheckToken call headerParam {} proxyParam {}", headerParam, proxyParam);
        String srcService = proxyParam.getSrcService();
        String msgId = proxyParam.getMsgId();
        String msgContent = proxyParam.getMsgContent();
        String userId = headerParam.getUserId();
        String token = headerParam.getToken();
        String appId = headerParam.getAppId();
        String channelId = headerParam.getChannelId();
        String sign = headerParam.getSign();
        String body = headerParam.getBody();
        if (ProxyCheckParamUtils.checkIsNullOrEmpty(srcService, msgId, msgContent, userId, token, appId, channelId)) {
            log.error("clientJsonMsgCheckToken is error headerParam {} proxyParam {}", headerParam, proxyParam);
            ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonProxyError.param_miss.getCode()).msg(HomoCommonProxyError.param_miss.message()).build();
            return Homo.result(JSON.toJSONString(responseMsg));
        }
        try {
            Boolean checkSignPass = checkSign(appId, msgId, body, sign);
            if (!checkSignPass) {
                log.error("clientJsonMsgCheckToken checkSignPass false headerParam {} proxyParam {}", headerParam, proxyParam);
                ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonProxyError.sign_error.getCode()).msg(HomoCommonProxyError.sign_error.message()).build();
                return Homo.result(JSON.toJSONString(responseMsg));
            }
            String serverPort = commonProxyProperties.getServerPortMap().get(srcService);
            String serviceName = srcService + ":" + serverPort;
            JSONArray paramArray = JSON.parseArray(msgContent);
            //把消息头添加到最后一个参数
            paramArray.add(headerParam);
            JsonRpcContent rpcContent = JsonRpcContent.builder().data(paramArray.toJSONString()).build();
            RpcAgentClient agentClient = rpcClientMgr.getGrpcServerlessAgentClient(serviceName);
            Homo<String> homo = agentClient.rpcCall(msgId, rpcContent)
                    .nextDo(ret -> {
                        String jsonRet = (String) ret;
                        log.info("proxyCall serviceName {} msgId {} rpcContent {} jsonRet {}", serviceName, msgId, rpcContent, jsonRet);
                        ResponseMsg responseMsg = ResponseMsg.builder()
                                .code(HomoCommonProxyError.success.getCode())
                                .msg(HomoCommonProxyError.success.message())
                                .msgId(msgId)
                                .msgContent(jsonRet)
                                .build();
                        return Homo.result(JSON.toJSONString(responseMsg));
                    });
            return homo.errorContinue(throwable -> {
                log.error("clientJsonMsgCheckToken rpcCall error headerParam {} proxyParam {} e", headerParam, proxyParam, throwable);
                ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonProxyError.common_system_error.getCode()).msg(HomoCommonProxyError.common_system_error.message()).build();
                return Homo.result(JSON.toJSONString(responseMsg));
            });
        } catch (Exception e) {
            log.error("clientJsonMsgCheckToken checkSign error headerParam {} proxyParam {} e", headerParam, proxyParam, e);
            ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonProxyError.common_system_error.getCode()).msg(HomoCommonProxyError.common_system_error.message()).build();
            return Homo.result(JSON.toJSONString(responseMsg));
        }
    }

    private Boolean checkSign(String appId, String msgId, String msgBody, String clientSign) throws NoSuchAlgorithmException {
        String secret = commonProxyProperties.getAppSecretKeyMap().get(appId);
        if (secret == null || "".equals(secret)) {
            log.error("checkSign fail appId {} msgId {} body {} secret {}", appId, msgId, msgBody, secret);
            return false;
        }
        String contentMd5 = ProxySignatureUtil.contentMd5(msgBody.getBytes());
        Map<String, String> param = new HashMap<>(4);
        param.put("appId", appId);
        param.put("messageId", msgId);
        param.put("content-md5", contentMd5);
        return ProxySignatureUtil.sign(param, secret).equals(clientSign);
    }



    @Override
    public Homo<byte[]> clientPbMsgCheckSign(ClientRouterMsg clientRouterMsg) {
        return null;
    }


}
