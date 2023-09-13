package com.homo.game.login.handler;

import com.alibaba.fastjson.JSONObject;
import com.homo.core.storage.ObjStorage;
import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.secret.EncryptUtils;
import com.homo.game.login.utils.LoginSignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class TokenHandler {
    @Autowired
    private ObjStorage storage;
    public static String TOKEN_LOGIC_TYPE = "security";

    public Homo<String> getStorageToken(String appId, String channelId, String userId) {
        return storage.load(appId, channelId, TOKEN_LOGIC_TYPE, userId, "token", String.class)
                .consumerValue(ret -> {
                    log.info("getStorageToken appId {} channelId {} userId {} token {}", appId, channelId, userId, ret);
                });
    }

    public Homo<Boolean> setStorageToken(String appId, String channelId, String userId, String token) {
        log.info("setStorageToken appId {} channelId {} userId {} token {}", appId, channelId, userId, token);
        return storage.save(appId, channelId, TOKEN_LOGIC_TYPE, userId, "token", token);
    }

    public Homo<Boolean> authToken(String appId, String channelId, String userId, String clientToken) {
        return getStorageToken(appId, channelId, userId)
                .nextDo(token -> {
                    return Homo.result(clientToken.equals(token));
                });
    }

    public Boolean checkSign(String appId, String msgId, byte[] msgBody, String clientSign) throws NoSuchAlgorithmException {
        String secret = "secret";
        if (secret == null || "".equals(secret)) {
            log.error("checkSign fail appId {} msgId {} body {} secret {}", appId, msgId, msgBody, secret);
            return false;
        }
        String contentMd5 = LoginSignatureUtil.contentMd5(msgBody);
        Map<String, String> param = new HashMap<>(4);
        param.put("appId", appId);
        param.put("messageId", msgId);
        param.put("content-md5", contentMd5);
        return LoginSignatureUtil.sign(param, secret).equals(clientSign);
    }
}
