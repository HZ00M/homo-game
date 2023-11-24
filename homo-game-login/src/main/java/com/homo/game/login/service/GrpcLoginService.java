package com.homo.game.login.service;

import com.homo.core.facade.module.RootModule;
import com.homo.core.rpc.base.service.BaseService;
import com.homo.core.utils.rector.Homo;
import com.homo.game.login.facade.IGrpcLoginService;
import com.homo.game.login.facade.dto.CommonCode;
import com.homo.game.login.facade.dto.Response;
import com.homo.game.login.handler.TokenHandler;
import com.homo.game.login.proto.Auth;
import io.homo.proto.client.ParameterMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GrpcLoginService extends BaseService implements IGrpcLoginService {
    @Autowired
    TokenHandler tokenHandler;
    @Autowired
    RootModule rootModule;

    @Override
    public Homo<Response> auth(Integer podIndex,ParameterMsg parameterMsg, Auth auth) {
        return tokenHandler.authToken(rootModule.getServerInfo().getAppId(), auth.getChannelId(), auth.getUserId(), auth.getToken())
                .nextDo(pass -> {
                    if (pass) {
                        Response success = Response.successed();
                        return Homo.result(success);
                    } else {
                        return Homo.result(Response.fail(CommonCode.FAIL_TLENL_ERROR, "auth fail"));
                    }
                });

    }

    @Override
    public Homo<com.homo.game.login.proto.Response> queryUserInfo(Integer podIndex,ParameterMsg parameterMsg,Auth auth) {
        log.info("queryUserInfo auth {}", auth);
        com.homo.game.login.proto.Response response = com.homo.game.login.proto.Response.newBuilder().setDesc("ok").build();
        return Homo.result(response);
    }

    @Override
    public Homo<Response> sendPhoneCode(Integer podIndex,ParameterMsg parameterMsg,Auth param, String phone, String fetchType) {
        Response success = Response.successed();
        return Homo.result(success);
    }

    @Override
    public Homo<Response> validatePhoneCode(Integer podIndex,ParameterMsg parameterMsg,Auth param, String phone, String code, String fetchType) {
        Response success = Response.successed();
        return Homo.result(success);
    }
}
