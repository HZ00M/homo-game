package com.homo.game.proxy.handler;

import com.alibaba.fastjson.JSONObject;
import com.homo.core.utils.concurrent.queue.CallQueueMgr;
import com.homo.core.utils.secret.EncryptUtils;
import com.homo.game.login.facade.IGrpcLoginService;
import com.homo.game.login.proto.Auth;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.core.utils.rector.Homo;
import com.homo.game.proxy.util.ProxySignatureUtil;
import io.homo.proto.client.Msg;
import io.homo.proto.client.ParameterMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AuthTokenHandler implements RouterHandler {
    @Value("#{${homo.common.homo.app.checkToken.appSecretKey.map:{}}}")
    private Map<String, String> appSecretKeyMap;
    //    @Autowired
//    private ObjStorage storage;
    @Autowired
    CheckUserNumberHandler userNumberHandler;
    //    public static String TOKEN_LOGIC_TYPE = "token";
    @Autowired
    IGrpcLoginService loginService;

    @Override
    public Integer order() {
        return 1;
    }

    @Override
    public Homo<Object> handler(HandlerContext context) {
        String appId = context.getParam(RouterHandler.PARAM_APP_ID, String.class);
        String channelId = context.getParam(RouterHandler.PARAM_CHANNEL_ID, String.class);
        String userId = context.getParam(RouterHandler.PARAM_USER_ID, String.class);
        String token = context.getParam(RouterHandler.PARAM_TOKEN, String.class);
//        final String userId = loginMsgReq.getUserId();
//        final String channelId = loginMsgReq.getChannelId();
//        final String authToken = loginMsgReq.getToken();
//        final String appVersion = loginMsgReq.getAppVersion();
//        final String resVersion = loginMsgReq.getResVersion();
//        final String adId = loginMsgReq.getAdId();
        return checkToken(appId, channelId, userId, token)
                .nextDo(authPass -> {
                            if (authPass) {
                                if (true) {//todo 简化逻辑  认证成功即算一人在线
                                    userNumberHandler.incrServerPlayerNumber();
                                }
                                Tuple2<Boolean, String> tuples = Tuples.of(true, "AuthTokenHandler success"); //todo 需要优化代码逻辑
                                context.promiseResult(tuples);

                            } else {
                                Tuple2<Boolean, String> tuples = Tuples.of(false, "AuthTokenHandler check fail");
                                context.promiseResult(tuples);
                            }
                            return context.handler(context);
                        }
                );
    }

    public Homo<Boolean> checkToken(String appId, String channelId, String userId, String token) {
        ParameterMsg parameterMsg = ParameterMsg.newBuilder().setChannelId(channelId).setUserId(userId).build();
        Auth auth = Auth.newBuilder().setChannelId(channelId).setUserId(userId).setToken(token).build();
        return Homo.result(true)
//                .switchThread(CallQueueMgr.getInstance().makeQueueId(userId.hashCode()))//todo 待验证
                .nextDo(res -> {
                            log.info("checkToken appId {} channelId {} userId {} token {}", appId, channelId, userId, token);
                            return loginService.auth(-1,parameterMsg, auth)
                                    .nextDo(ret -> {
                                        log.info("checkToken auth userId {} ret {}", userId, ret);
                                        return Homo.result(ret.checkSuccess());
                                    });
                        }
                )
                ;
    }


//    public Homo<String> getStorageToken(String appId, String channelId, String userId) {
//        return storage.load(appId, channelId, TOKEN_LOGIC_TYPE, userId, "token", String.class);
//    }
//
//    public Homo<Boolean> setStorageToken(String appId, String channelId, String userId, String token) {
//        return storage.save(appId, channelId, TOKEN_LOGIC_TYPE, userId, "token", token);
//    }

//    private Homo<Boolean> authToken(String appId, String channelId, String userId, String token, String storageToken) {
//        if (!token.equals(storageToken)) {
//            //todo 需要通过authUrl向认证服务器认证token, 这里简化逻辑，内部认证token
//            JSONObject payload = new JSONObject();
//            payload.put("id", userId);
//            payload.put("token", token);
//            String originSign = String.format("appId=%s&channelId=%s&id=%s&secret=%s", appId, channelId, userId, appSecretKeyMap.get(appId));
//            String realSign = EncryptUtils.md5(originSign);
//            payload.put("sign", realSign);
//            Map<String, String> headers = new HashMap<>();
//            headers.put("appId", appId);
//            headers.put("channelId", channelId);
//            JSONObject authRet = authToken(headers, payload);
//            int errorCode = authRet.getIntValue("errorCode");
//            if (errorCode == 0) {
//                return setStorageToken(appId, channelId, userId, token).nextDo(ret -> {
//                    return Homo.result(true);
//                });
//            } else {
//                return Homo.result(false);
//            }
//        } else {
//            return Homo.result(true);
//        }
//    }


    private JSONObject authToken(Map<String, String> headers, JSONObject payload) {
        //todo 这里本需要登陆平台验证，简化流程
        String sign = payload.getString("sign");
        //签名算法要对齐
        String originSign = String.format("appId=%s&channelId=%s&id=%s&secret=%s", headers.get("appId"), headers.get("channelId"), payload.getString("id"), appSecretKeyMap.get(headers.get("appId")));
        JSONObject result = new JSONObject();
        String authSign = EncryptUtils.md5(originSign);
        if (!sign.equals(authSign)) {
            result.put("errorCode", 1);
        } else {
            result.put("errorCode", 0);
        }
        return result;
    }

    public Boolean checkSign(String appId, String msgId, byte[] msgBody, String clientSign) throws NoSuchAlgorithmException {
        String secret = appSecretKeyMap.get(appId);
        if (secret == null || "".equals(secret)) {
            log.error("checkSign fail appId {} msgId {} body {} secret {}", appId, msgId, msgBody, secret);
            return false;
        }
        String contentMd5 = ProxySignatureUtil.contentMd5(msgBody);
        Map<String, String> param = new HashMap<>(4);
        param.put("appId", appId);
        param.put("messageId", msgId);
        param.put("content-md5", contentMd5);
        return ProxySignatureUtil.sign(param, secret).equals(clientSign);
    }
}
