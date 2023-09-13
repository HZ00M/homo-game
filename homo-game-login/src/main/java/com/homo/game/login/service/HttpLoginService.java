package com.homo.game.login.service;

import com.alibaba.fastjson.JSONObject;
import com.homo.core.rpc.base.service.BaseService;
import com.homo.core.utils.rector.Homo;
import com.homo.game.login.facade.IHttpLoginService;
import com.homo.game.login.facade.dto.Response;
import com.homo.game.login.handler.TokenHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HttpLoginService extends BaseService implements IHttpLoginService {
    @Autowired
    TokenHandler tokenHandler;
    @Override
    public Homo<String> register(JSONObject param, JSONObject header) {
        Response success = Response.successed();
        return Homo.result(JSONObject.toJSONString(success));
    }

    @Override
    public Homo<String> login(JSONObject param, JSONObject header) {
        return tokenHandler.setStorageToken(header.getString("X-Homo-App-Id"),header.getString("X-Homo-Channel-Id"),param.getString("userId"),header.getString("X-Homo-Token"))
                .nextDo(ret->{
                    Response success = Response.successed();
                    return Homo.result(JSONObject.toJSONString(success));
                });
    }
}
