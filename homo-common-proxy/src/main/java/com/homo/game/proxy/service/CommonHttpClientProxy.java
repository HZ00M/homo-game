package com.homo.game.proxy.service;

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
import com.homo.core.facade.service.ServiceStateMgr;
import com.homo.core.rpc.http.dto.ResponseMsg;
import com.homo.core.utils.concurrent.queue.CallQueue;
import com.homo.core.utils.concurrent.queue.CallQueueMgr;
import com.homo.core.utils.exception.HomoError;
import com.homo.core.utils.http.HttpCallerFactory;
import com.homo.core.utils.module.ServiceModule;
import com.homo.game.proxy.config.CommonProxyProperties;
import com.homo.game.proxy.dto.HeaderParam;
import com.homo.game.proxy.dto.ProxyParam;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.game.proxy.enums.ProxyKey;
import com.homo.game.proxy.facade.ICommonHttpClientProxy;
import com.homo.game.proxy.handler.AuthTokenHandler;
import com.homo.game.proxy.util.ProxyCheckParamUtils;
import com.homo.core.facade.rpc.RpcAgentClient;
import com.homo.core.rpc.base.serial.ByteRpcContent;
import com.homo.core.rpc.base.serial.JsonRpcContent;
import com.homo.core.rpc.base.service.BaseService;
import com.homo.core.rpc.client.RpcClientMgr;

import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.trace.ZipkinUtil;
import io.grpc.StatusRuntimeException;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;
import io.homo.proto.rpc.Req;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import reactor.util.function.Tuple2;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CommonHttpClientProxy extends BaseService implements ICommonHttpClientProxy, ServiceModule {
    private static final String RESOURCE_HTTP_FORWARD = "httpForward";
    @Autowired
    private CommonProxyProperties commonProxyProperties;
    @Autowired
    private HttpCallerFactory httpCallerFactory;
    @Autowired
    private RpcClientMgr rpcClientMgr;
    @Autowired
    private AuthTokenHandler tokenHandler;
    @Autowired
    ServiceStateMgr serviceStateMgr;

    @Override
    public void afterAllModuleInit() {
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new ApolloDataSource<>(commonProxyProperties.getDatasourceNamespace(), "flowRules",
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
    public Homo<String> httpForward(JSONObject requestJson, JSONObject headerJson) {
        try {
            log.info("httpForward begin headerJson {} requestJson {}", headerJson, requestJson);

            AsyncEntry entry;
            String method = requestJson.getString(ProxyKey.METHOD_KEY);
            String url = requestJson.getString(ProxyKey.URL_KEY);
            String msg = requestJson.getString(ProxyKey.MSG_KEY);
            String[] routes = url.split("/");
            String forwardKey = routes[2];
            if (ProxyCheckParamUtils.checkIsNullOrEmpty(method, url, msg, forwardKey)) {
                log.error("clientJsonMsgCheckToken checkIsNullOrEmpty error headerJson {} requestJson {}", headerJson, requestJson);
                ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonError.param_miss.getCode()).codeDesc(HomoCommonError.param_miss.message()).build();
                return Homo.result(JSON.toJSONString(responseMsg));
            }
            if (commonProxyProperties.getFlowControlKeys().contains(forwardKey)) {
                entry = SphU.asyncEntry(RESOURCE_HTTP_FORWARD);
            } else {
                entry = null;
            }
            Homo<Boolean> canForwardPromise;
            if (commonProxyProperties.getIgnoreCheckTokenKeys().contains(forwardKey)) {
                //login-service-http登陆服请求不校验token
                canForwardPromise = Homo.result(true);
            } else {
                String appId = headerJson.getString(ProxyKey.X_HOMO_APP_ID);
                String token = headerJson.getString(ProxyKey.X_HOMO_TOKEN);
                String userId = headerJson.getString(ProxyKey.X_HOMO_USER_ID);
                String channelId = headerJson.getString(ProxyKey.X_HOMO_CHANNEL_ID);
                if (ProxyCheckParamUtils.checkIsNullOrEmpty(appId, token, userId, channelId)) {
                    log.error("clientJsonMsgCheckToken checkIsNullOrEmpty error appId {} token {} userId {} channelId {}",
                            appId, token, userId, channelId);
                    ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonError.param_miss.getCode()).codeDesc(HomoCommonError.param_miss.message()).build();
                    return Homo.result(JSON.toJSONString(responseMsg));
                }
                canForwardPromise = tokenHandler.checkToken(appId, channelId, userId, token);
            }
            return canForwardPromise
                    .nextDo(pass -> {
                        if (!pass) {
                            log.error("check token error headersJson_{} requestJson_{}", headerJson, requestJson);
                            ResponseMsg response = ResponseMsg.builder().code(HomoCommonError.token_error.getCode()).codeDesc(HomoCommonError.token_error.message()).build();
//                            JSONObject responseHeader = new JSONObject();
//                            responseHeader.put(ProxyKey.X_HOMO_RESPONSE, "check token error");
//                            HttpParam httpParam = HttpParam.builder().headers(responseHeader.toJSONString()).code(HttpStatus.OK.value()).result(JSON.toJSONString(response)).build();
                            return Homo.result(JSONObject.toJSONString(response));
                        }
                        String forwardUrl = commonProxyProperties.getForwardUrlMap().get(forwardKey);
                        if (forwardUrl == null) {
                            log.error("forwardUrl == null requestJson_{}", requestJson);
                            ResponseMsg response = ResponseMsg.builder().code(HomoCommonError.no_forward_url.getCode()).codeDesc(HomoCommonError.no_forward_url.msgFormat(forwardKey)).build();
//                            HttpParam httpParam = HttpParam.builder().code(HttpStatus.OK.value()).result(JSON.toJSONString(response)).build();
                            return Homo.result(JSONObject.toJSONString(response));
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

                        return httpCall(entry, forwardUrl, forwardUrlWithParam, method, msg, headerJson, requestJson);
                    })
                    .consumerValue(ret -> {
                        log.info("httpForward end headerJson {} requestJson {}", headerJson, requestJson);
                    });
        } catch (BlockException e) {
            log.error("httpForward over limit count headerJson {} requestJson {}", headerJson, requestJson);
            ResponseMsg responseMsg = ResponseMsg.builder()
                    .code(HomoCommonError.flow_limit_error.getCode())
                    .codeDesc(HomoCommonError.flow_limit_error.msgFormat(e.getMessage()))
                    .build();
            return Homo.result(JSONObject.toJSONString(responseMsg));
        } catch (Exception e) {
            log.error("httpForward system error {} {}", headerJson, requestJson, e);
            ResponseMsg responseMsg = ResponseMsg.builder()
                    .code(HomoCommonError.common_system_error.getCode())
                    .codeDesc(HomoCommonError.common_system_error.msgFormat(e.getMessage()))
                    .build();
            return Homo.result(JSONObject.toJSONString(responseMsg));
        }
    }

    private Homo<String> httpCall(AsyncEntry asyncEntry, String host, String url, String method, String msg, JSONObject headersJson, JSONObject requestJson) {
        CallQueue queue = CallQueueMgr.getInstance().getLocalQueue();
        Span span = ZipkinUtil.getTracing()
                .tracer()
                .nextSpan()
                .name(method)
                .tag("url", url)
                .annotate(ZipkinUtil.CLIENT_SEND_TAG);
        long traceId = span.context().traceId();
        long spanId = span.context().spanId();
        Boolean sampled = span.context().sampled();
        return Homo.warp(() -> {
            log.info("httpCall start host {} url {} method {} msg {} headersJson {} requestJson {}", host, url, method, msg, headersJson, requestJson);
            String responseTimeStr = headersJson.getString(ProxyKey.X_HOMO_RESPONSE_TIME);
            long responseTime = responseTimeStr == null ? 10000 : Long.parseLong(responseTimeStr);
            headersJson.remove("Host");
            headersJson.put("traceId", String.valueOf(traceId));
            headersJson.put("spanId", String.valueOf(spanId));
            headersJson.put("sampled", String.valueOf(sampled));
            HttpClient httpClient = httpCallerFactory.getHttpClientCache(host);
            Mono<ResponseMsg> httpParamMono = httpClient
                    .followRedirect(true)//不允许重定向
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
                                        ResponseMsg responseMsg = ResponseMsg.builder()
                                                .code(code)
                                                .codeDesc(respHeader.toJSONString())
                                                .msgContent(resp)
                                                .build();
                                        sink.success(responseMsg);
                                    });
                        });
                    });
            return Homo.warp(() -> httpParamMono)
                    .switchThread(queue, span)
                    .nextDo(httpParam -> {
                        if (span != null) {
                            span.annotate(ZipkinUtil.CLIENT_RECEIVE_TAG)
                                    .tag("httpCode", httpParam.getCode() + "")
                                    .finish();
                        }
                        if (asyncEntry != null) {
                            asyncEntry.exit();
                        }
                        log.info("httpCall end code {} url {} body {}", httpParam.getCode(), url, msg);
                        return Homo.result(JSON.toJSONString(httpParam));
                    })
                    .onErrorContinue(throwable -> {
                        if (span != null) {
                            span.annotate("HttpCall error").error(throwable);
                        }
                        if (asyncEntry != null) {
                            asyncEntry.exit();
                        }
                        ResponseMsg responseMsg;
                        if (throwable instanceof StatusRuntimeException) {
                            responseMsg = ResponseMsg.builder().code(HomoCommonError.remote_server_no_response.getCode()).codeDesc(HomoCommonError.remote_server_no_response.msgFormat(host)).build();
                        } else {
                            responseMsg = ResponseMsg.builder().code(HomoCommonError.common_system_error.getCode()).codeDesc(HomoCommonError.common_system_error.msgFormat(throwable.getMessage())).build();
                        }
                        log.error("httpCall error headersJson {} requestJson {}", headersJson, requestJson, throwable);
                        return Homo.result(JSONObject.toJSONString(responseMsg));
                    });
        });
    }

    @Override
    public Homo<String> clientJsonMsgCheckToken(ProxyParam proxyParam, HeaderParam headerParam) {
        log.info("clientJsonMsgCheckToken call headerParam {} proxyParam {}", headerParam, proxyParam);
        String srcService = proxyParam.getSrcService();
        String msgId = proxyParam.getMsgId();
        String msgContent = proxyParam.getMsgContent();
        String userId = headerParam.getUserId();
        String token = headerParam.getToken();
        String appId = headerParam.getAppId();
        String channelId = headerParam.getChannelId();
        if (ProxyCheckParamUtils.checkIsNullOrEmpty(srcService, msgId, msgContent, userId, token, appId, channelId)) {
            log.error("clientJsonMsgCheckToken checkIsNullOrEmpty error headerParam {} proxyParam {}", headerParam, proxyParam);
            ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonError.param_miss.getCode()).codeDesc(HomoCommonError.param_miss.message()).build();
            return Homo.result(JSON.toJSONString(responseMsg));
        }

        return tokenHandler.checkToken(appId, token, userId, channelId)
                .nextDo(checkPass -> {
                    if (!checkPass) {
                        log.error("clientJsonMsgCheckToken checkPass error headerParam {} proxyParam {}", headerParam, proxyParam);
                        ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonError.token_error.getCode()).codeDesc(HomoCommonError.token_error.message()).build();
                        return Homo.result(JSON.toJSONString(responseMsg));
                    }
                    JSONArray paramArray = new JSONArray();
                    paramArray.add(msgContent);
                    //把消息头添加到最后一个参数
                    paramArray.add(headerParam);
                    JsonRpcContent rpcContent = new JsonRpcContent();
                    rpcContent.setParam(paramArray.toJSONString());
                    String serverPort = commonProxyProperties.getServerPortMap().get(srcService);
                    String serviceName = srcService + ":" + serverPort;
                    return serviceStateMgr.getServiceInfo(srcService)
                            .nextDo(serviceInfo -> {
                                RpcAgentClient agentClient = rpcClientMgr.getAgentClient(serviceName, serviceInfo);
                                Homo<String> homo = agentClient.rpcCall(msgId, rpcContent)
                                        .nextDo(ret -> {
                                            String retData = rpcContent.getReturn();
                                            log.info("proxyCall serviceName {} msgId {} rpcContent {} jsonRet {}", serviceName, msgId, rpcContent, ret);
                                            ResponseMsg responseMsg = ResponseMsg.builder()
                                                    .code(HomoError.success.getCode())
                                                    .codeDesc(HomoError.success.message())
                                                    .msgId(msgId)
                                                    .msgContent(retData)
                                                    .build();
                                            return Homo.result(JSON.toJSONString(responseMsg));
                                        });
                                return homo;
                            });

                })
                .errorContinue(throwable -> {
                    log.error("clientJsonMsgCheckToken system error headerParam {} proxyParam {} e", headerParam, proxyParam, throwable);
                    ResponseMsg responseMsg;
                    if (throwable instanceof StatusRuntimeException) {
                        responseMsg = ResponseMsg.builder().code(HomoCommonError.common_system_error.getCode()).codeDesc(HomoCommonError.common_system_error.message()).build();
                    } else {
                        responseMsg = ResponseMsg.builder().code(HomoCommonError.remote_server_no_response.getCode()).codeDesc(HomoCommonError.remote_server_no_response.msgFormat(srcService)).build();
                    }
                    return Homo.result(JSON.toJSONString(responseMsg));
                });
    }

    @Override
    public Homo<Msg> clientPbMsgCheckToken(ClientRouterMsg clientRouterMsg) {
        String srcService = clientRouterMsg.getSrcService();
        String msgId = clientRouterMsg.getMsgId();
        String userId = clientRouterMsg.getUserId();
        String token = clientRouterMsg.getToken();
        String appId = clientRouterMsg.getAppId();
        String channelId = clientRouterMsg.getChannelId();
        log.info("clientPbMsgCheckToken userId {} srcService {} msgId {} appId {} channelId {}",
                userId, srcService, msgId, appId, channelId);
        if (ProxyCheckParamUtils.checkIsNullOrEmpty(srcService, msgId, userId, token, appId, channelId)) {
            Msg responseMsg = Msg.newBuilder().setCode(HomoCommonError.param_miss.getCode()).setCodeDesc(HomoCommonError.param_miss.message()).build();
            return Homo.result(responseMsg);
        }

        return tokenHandler.checkToken(appId, token, userId, channelId)
                .nextDo(checkPass -> {
                    if (!checkPass) {
                        log.info("clientPbMsgCheckToken !checkPass userId {} srcService {} msgId {} appId {} channelId {}",
                                userId, srcService, msgId, appId, channelId);
                        Msg responseMsg = Msg.newBuilder().setCode(HomoCommonError.token_error.getCode()).setCodeDesc(HomoCommonError.token_error.message()).build();
                        return Homo.result(responseMsg);
                    }
                    return serviceStateMgr.getServiceInfo(srcService)
                            .nextDo(serviceInfo -> {
                                List<ByteString> msgContentList = clientRouterMsg.getMsgContentList();
                                byte[][] params = new byte[msgContentList.size()][];
                                for (ByteString bytes : msgContentList) {
                                    params[0] = bytes.toByteArray();
                                }
                                String serverPort = commonProxyProperties.getServerPortMap().get(srcService);
                                String serviceName = srcService + ":" + serverPort;
                                ByteRpcContent rpcContent = new ByteRpcContent();
                                rpcContent.setParam(params);
                                RpcAgentClient agentClient = rpcClientMgr.getAgentClient(serviceName, serviceInfo);
                                Homo<Msg> homo = agentClient.rpcCall(msgId, rpcContent)
                                        .nextDo(ret -> {
                                            byte[] data = rpcContent.getReturn();
                                            log.info("proxyCall serviceName {} msgId {} ", serviceName, msgId);
                                            Msg responseMsg = Msg.newBuilder()
                                                    .setCode(HomoError.success.getCode())
                                                    .setCodeDesc(HomoError.success.message())
                                                    .setMsgId(msgId)
                                                    .setMsgContent(ByteString.copyFrom(data))
                                                    .build();
                                            return Homo.result(responseMsg);
                                        });
                                return homo;
                            });

                })
                .errorContinue(throwable -> {
                    log.error("clientPbMsgCheckToken system error clientRouterMsg {}  e", clientRouterMsg, throwable);
                    Msg responseMsg;
                    if (throwable instanceof StatusRuntimeException) {
                        responseMsg = Msg.newBuilder().setCode(HomoCommonError.remote_server_no_response.getCode()).setCodeDesc(HomoCommonError.remote_server_no_response.msgFormat(srcService)).build();
                    } else {
                        responseMsg = Msg.newBuilder().setCode(HomoCommonError.common_system_error.getCode()).setCodeDesc(HomoCommonError.common_system_error.message()).build();

                    }
                    return Homo.result(responseMsg);
                });
    }

    @Override
    public Homo<String> clientJsonMsgCheckSign(ProxyParam proxyParam, HeaderParam headerParam) {
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
            ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonError.param_miss.getCode()).codeDesc(HomoCommonError.param_miss.message()).build();
            return Homo.result(JSON.toJSONString(responseMsg));
        }
        try {
            Boolean checkSignPass = tokenHandler.checkSign(appId, msgId, body.getBytes(StandardCharsets.UTF_8), sign);
            if (!checkSignPass) {
                log.error("clientJsonMsgCheckToken checkSignPass false headerParam {} proxyParam {}", headerParam, proxyParam);
                ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonError.sign_error.getCode()).codeDesc(HomoCommonError.sign_error.message()).build();
                return Homo.result(JSON.toJSONString(responseMsg));
            }
            String serverPort = commonProxyProperties.getServerPortMap().get(srcService);
            String serviceName = srcService + ":" + serverPort;
            JSONArray paramArray = new JSONArray();
            paramArray.add(msgContent);
            //把消息头添加到最后一个参数
            paramArray.add(headerParam);
            JsonRpcContent rpcContent = new JsonRpcContent();
            rpcContent.setParam(paramArray.toJSONString());
            return serviceStateMgr.getServiceInfo(srcService)
                    .nextDo(serviceInfo -> {
                        RpcAgentClient agentClient = rpcClientMgr.getAgentClient(serviceName, serviceInfo);
                        Homo<String> homo = agentClient.rpcCall(msgId, rpcContent)
                                .nextDo(ret -> {
                                    String retData = rpcContent.getReturn();
                                    log.info("proxyCall serviceName {} msgId {} rpcContent {} jsonRet {}", serviceName, msgId, rpcContent, retData);
                                    ResponseMsg responseMsg = ResponseMsg.builder()
                                            .code(HomoError.success.getCode())
                                            .codeDesc(HomoError.success.message())
                                            .msgId(msgId)
                                            .msgContent(retData)
                                            .build();
                                    return Homo.result(JSON.toJSONString(responseMsg));
                                });
                        return homo.errorContinue(throwable -> {
                            log.error("clientJsonMsgCheckSign rpcCall error headerParam {} proxyParam {} e", headerParam, proxyParam, throwable);
                            ResponseMsg responseMsg;
                            if (throwable instanceof StatusRuntimeException) {
                                responseMsg = ResponseMsg.builder().code(HomoCommonError.remote_server_no_response.getCode()).codeDesc(HomoCommonError.remote_server_no_response.msgFormat(srcService)).build();
                            } else {
                                responseMsg = ResponseMsg.builder().code(HomoCommonError.common_system_error.getCode()).codeDesc(HomoCommonError.common_system_error.message()).build();
                            }
                            return Homo.result(JSON.toJSONString(responseMsg));
                        });
                    });

        } catch (Exception e) {
            log.error("clientJsonMsgCheckSign system error headerParam {} proxyParam {} e", headerParam, proxyParam, e);
            ResponseMsg responseMsg = ResponseMsg.builder().code(HomoCommonError.common_system_error.getCode()).codeDesc(HomoCommonError.common_system_error.message()).build();
            return Homo.result(JSON.toJSONString(responseMsg));
        }
    }

    @Override
    public Homo<Msg> clientPbMsgCheckSign(ClientRouterMsg clientRouterMsg) {
        String srcService = clientRouterMsg.getSrcService();
        String msgId = clientRouterMsg.getMsgId();
        String userId = clientRouterMsg.getUserId();
        String token = clientRouterMsg.getToken();
        String appId = clientRouterMsg.getAppId();
        String channelId = clientRouterMsg.getChannelId();
        String clientSign = clientRouterMsg.getSign();
        Req.Builder body = Req.newBuilder();
        log.info("clientPbMsgCheckSign srcService {} msgId {} userId {} token {} appId {} channelId {} clientSign {}",
                srcService, msgId, userId, token, appId, channelId, clientSign);
        body.setMsgId(msgId).setSrcService(srcService);
        for (ByteString bytes : clientRouterMsg.getMsgContentList()) {
            body.addMsgContent(bytes);
        }
        byte[] bodyByteArr = body.build().toByteArray();
        if (ProxyCheckParamUtils.checkIsNullOrEmpty(srcService, msgId, userId, token, appId, channelId)) {
            log.error("clientPbMsgCheckSign checkIsNullOrEmpty error srcService {} msgId {} userId {} token {} appId {} channelId {} clientSign {} ",
                    srcService, msgId, userId, token, appId, channelId, clientSign);
            Msg responseMsg = Msg.newBuilder().setCode(HomoCommonError.param_miss.getCode()).setCodeDesc(HomoCommonError.param_miss.message()).build();
            return Homo.result(responseMsg);
        }
        try {
            Boolean checkSignPass = tokenHandler.checkSign(appId, msgId, bodyByteArr, clientSign);
            if (!checkSignPass) {
                log.error("clientPbMsgCheckSign checkSignPass error  {} msgId {} userId {} token {} appId {} channelId {} clientSign {} ",
                        srcService, msgId, userId, token, appId, channelId, clientSign);
                Msg responseMsg = Msg.newBuilder().setCode(HomoCommonError.sign_error.getCode()).setCodeDesc(HomoCommonError.sign_error.message()).build();
                return Homo.result(responseMsg);
            }
            List<ByteString> msgContentList = clientRouterMsg.getMsgContentList();
            byte[][] params = new byte[msgContentList.size()][];
            for (ByteString bytes : msgContentList) {
                params[0] = bytes.toByteArray();
            }
            String serverPort = commonProxyProperties.getServerPortMap().get(srcService);
            String serviceName = srcService + ":" + serverPort;
            ByteRpcContent rpcContent = new ByteRpcContent();
            rpcContent.setParam(params);
            return serviceStateMgr.getServiceInfo(srcService)
                    .nextDo(serviceInfo -> {
                        RpcAgentClient agentClient = rpcClientMgr.getAgentClient(serviceName,serviceInfo);
                        Homo<Msg> homo = agentClient.rpcCall(msgId, rpcContent)
                                .nextDo(ret -> {
                                    byte[] data = rpcContent.getReturn();
                                    log.info("clientPbMsgCheckSign ret srcService {} msgId {} userId {} token {} appId {} channelId {} clientSign {}",
                                            srcService, msgId, userId, token, appId, channelId, clientSign);
                                    Msg responseMsg = Msg.newBuilder()
                                            .setCode(HomoError.success.getCode())
                                            .setCodeDesc(HomoError.success.message())
                                            .setMsgId(msgId)
                                            .setMsgContent(ByteString.copyFrom(data))
                                            .build();
                                    return Homo.result(responseMsg);
                                })
                                .errorContinue(throwable -> {
                                    log.error("clientPbMsgCheckSign system error clientRouterMsg {}  e", clientRouterMsg, throwable);
                                    Msg responseMsg = Msg.newBuilder().setCode(HomoCommonError.common_system_error.getCode()).setCodeDesc(HomoCommonError.common_system_error.message()).build();
                                    return Homo.result(responseMsg);
                                });
                        return homo;
                    });

        } catch (Exception e) {
            log.error("clientPbMsgCheckSign system error  {} msgId {} userId {} token {} appId {} channelId {} clientSign {} ",
                    srcService, msgId, userId, token, appId, channelId, clientSign, e);
            Msg responseMsg;
            if (e instanceof StatusRuntimeException) {
                responseMsg = Msg.newBuilder().setCode(HomoCommonError.remote_server_no_response.getCode()).setCodeDesc(HomoCommonError.remote_server_no_response.msgFormat(srcService)).build();
            } else {
                responseMsg = Msg.newBuilder().setCode(HomoCommonError.common_system_error.getCode()).setCodeDesc(HomoCommonError.common_system_error.message()).build();
            }
            return Homo.result(responseMsg);
        }
    }


}
