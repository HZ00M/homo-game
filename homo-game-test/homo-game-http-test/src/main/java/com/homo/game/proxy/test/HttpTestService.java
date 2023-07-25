package com.homo.game.proxy.test;

import com.alibaba.fastjson.JSONObject;
import com.homo.core.rpc.base.service.BaseService;
import com.homo.core.utils.rector.Homo;
import com.homo.game.proxy.test.facade.IHttpTestService;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.HttpTestReq;
import io.homo.proto.client.HttpTestRsp;
import io.homo.proto.client.ParameterMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HttpTestService extends BaseService implements IHttpTestService {
    @Override
    public Homo<String> jsonGetTest1(JSONObject params, JSONObject header) {
        log.info("jsonGetTest1 params {} header {} ", params, header);
        return Homo.result("success");
    }

    @Override
    public Homo<String> jsonGetTest2(String params, JSONObject header) {
        log.info("jsonGetTest2 params {} header {} ", params, header);
        return Homo.result("success");
    }


    @Override
    public Homo<String> jsonPostTest1(JSONObject params, JSONObject header) {
        log.info("jsonPostTest1 params {} header {} ", params, header);
        return Homo.result("success");
    }

    @Override
    public Homo<String> jsonPostTest2(String params, JSONObject header) {
        log.info("jsonPostTest2 params {} header {} ", params, header);
        return Homo.result("success");
    }

    @Override
    public Homo<String> jsonPostTest3(String params) {
        log.info("jsonPostTest3 params {} ", params);
        return Homo.result("success");
    }

    @Override
    public Homo<String> jsonPostTest4(JSONObject param1, JSONObject param2, JSONObject header) {
        log.info("jsonPostTest1 param1 {} param2 {} header {} ", param1, param2, header);
        return Homo.result("success");
    }

    @Override
    public Homo<String> jsonPostTest5(Integer podIndex, ParameterMsg parameterMsg, String params, JSONObject header) {
        log.info("jsonPostTest5 podIndex {} parameterMsg {} params {} header {}", podIndex, parameterMsg, params,header);
        return Homo.result("success");
    }

    @Override
    public Homo<HttpTestRsp> protoPostTest1(HttpTestReq req, ClientRouterHeader header) {
        log.info("protoPostTest1 params {} header {} ", req, header);
        return Homo.result(HttpTestRsp.newBuilder().setCode(1).setMsg("success").build());
    }

    @Override
    public Homo<HttpTestRsp> protoPostTest2(HttpTestReq req) {
        log.info("jsonGetTest2 params {} ", req);
        return Homo.result(HttpTestRsp.newBuilder().setCode(1).setMsg("success").build());
    }
}
