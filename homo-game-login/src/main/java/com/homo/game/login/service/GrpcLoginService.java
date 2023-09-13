package com.homo.game.login.service;

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

    @Override
    public Homo<Response> auth(ParameterMsg parameterMsg, Auth auth) {
        return tokenHandler.authToken(auth.getAppId(),auth.getChannelId(),auth.getUserId(),auth.getToken())
                .nextDo(pass->{
                    if (pass){
                        Response success = Response.successed();
                        return Homo.result(success);
                    }else {
                        return Homo.result(Response.fail(CommonCode.FAIL_TLENL_ERROR,"auth fail"));
                    }
                });

    }

    @Override
    public Homo<Response> queryUserInfo(Auth auth) {
        Response success = Response.successed();
        return Homo.result(success);
    }

    @Override
    public Homo<Response> sendPhoneCode(Auth param, String phone, String fetchType) {
        Response success = Response.successed();
        return Homo.result(success);
    }

    @Override
    public Homo<Response> validatePhoneCode(Auth param, String phone, String code, String fetchType) {
        Response success = Response.successed();
        return Homo.result(success);
    }
}
