package com.homo.game.proxy.handler;

import com.alibaba.fastjson.JSONObject;
import com.homo.core.storage.ObjStorage;
import com.homo.core.utils.secret.EncryptUtils;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.core.utils.rector.Homo;
import com.homo.game.proxy.util.ProxySignatureUtil;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Component
@Log4j2
public class AuthTokenHandler implements ProxyHandler {
    @Value("#{${homo.common.homo.app.checkToken.appSecretKey.map:{}}}")
    private Map<String, String> appSecretKeyMap;
    @Autowired
    private ObjStorage storage;
    @Autowired
    CheckUserNumberHandler userNumberHandler;

    @Override
    public Integer order() {
        return 1;
    }

    @Override
    public Homo<Void> handler(HandlerContext context) {
        ClientRouterMsg routerMsg = context.getRouterMsg();
        ClientRouterHeader header = context.getHeader();
        String appId = routerMsg.getAppId();
        String channelId = routerMsg.getChannelId();
        String userId = routerMsg.getUserId();
        String clientToken = routerMsg.getToken();
        return getStorageToken(appId, channelId, userId)
                .nextDo(storageToken -> {
                    return authToken(appId, channelId, userId, clientToken, storageToken)
                            .nextDo(authPass -> {
                                if (authPass) {
                                    if (storageToken == null) {
                                        userNumberHandler.incrServerPlayerNumber();
                                    }
                                    return context.handler(context);
                                } else {
                                    context.success(Msg.newBuilder().setMsgId(HomoCommonError.token_error.name()).build());
                                    return Homo.resultVoid();
                                }
                            });
                });
    }

    public Homo<String> getStorageToken(String appId, String channelId, String userId) {
        return storage.load(appId, channelId, "frameLogic", userId, "token", String.class);
    }

    public Homo<Boolean> setStorageToken(String appId, String channelId, String userId, String token) {
        return storage.save(appId, channelId, "frameLogic", userId, "token", token);
    }

    private Homo<Boolean> authToken(String appId, String channelId, String userId, String token, String storageToken) {
        if (!token.equals(storageToken)) {
            //todo 需要通过authUrl向认证服务器认证token, 这里简化逻辑，内部认证token
            JSONObject payload = new JSONObject();
            payload.put("id", userId);
            payload.put("token", token);
            String originSign = String.format("appId=%s&channelId=%s&id=%s&secret=%s", appId, channelId, userId, appSecretKeyMap.get(appId));
            String realSign = EncryptUtils.md5(originSign);
            payload.put("sign", realSign);
            Map<String, String> headers = new HashMap<>();
            headers.put("appId", appId);
            headers.put("channelId", channelId);
            JSONObject authRet = authToken(headers, payload);
            int errorCode = authRet.getIntValue("errorCode");
            if (errorCode == 0) {
                return setStorageToken(appId, channelId, userId, token).nextDo(ret -> {
                    return Homo.result(true);
                });
            } else {
                return Homo.result(false);
            }
        } else {
            return Homo.result(true);
        }
    }

    public Homo<Boolean> checkToken(String appId, String channelId, String userId, String token) {
        return getStorageToken(appId, channelId, userId)
                .nextDo(saveToken -> {
                    return authToken(appId, channelId, userId, token, saveToken);
                });
    }


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
