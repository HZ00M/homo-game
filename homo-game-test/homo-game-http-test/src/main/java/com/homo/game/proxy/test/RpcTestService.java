package com.homo.game.proxy.test;

import com.alibaba.fastjson.JSONObject;
import com.homo.core.rpc.base.service.BaseService;
import com.homo.core.utils.rector.Homo;
import com.homo.game.proxy.test.facade.IRpcTestService;
import io.homo.proto.client.HttpTestReq;
import io.homo.proto.client.HttpTestRsp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RpcTestService extends BaseService implements IRpcTestService {

    @Override
    public Homo<String> jsonPostTest1(JSONObject params, JSONObject header) {
        log.info("jsonPostTest1 params {} header {} ", params, header);
        return Homo.result("success");
    }

    @Override
    public Homo<HttpTestRsp> protoPostTest1(HttpTestReq req) {
        log.info("protoPostTest1 params {} ", req);
        return Homo.result(HttpTestRsp.newBuilder().setCode(1).setMsg("success").build());
    }

}
