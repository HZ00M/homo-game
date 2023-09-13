package com.homo.entity.test.service;

import com.alibaba.fastjson.JSONObject;
import com.homo.core.facade.ability.AbilityEntityMgr;
import com.homo.core.rpc.base.service.BaseService;
import com.homo.core.utils.rector.Homo;
import com.homo.entity.test.entity.UserEntity;
import com.homo.entity.test.facade.service.IUserService;
import io.homo.proto.client.ParameterMsg;
import io.homo.proto.client.TestReq;
import io.homo.proto.client.TestRsp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserService extends BaseService implements IUserService {
    @Autowired
    AbilityEntityMgr abilityEntityMgr;

    @Override
    public void postInit() {
        abilityEntityMgr.registerEntityNotFoundProcess(UserEntity.class, ((aClass, id) -> abilityEntityMgr.createEntityPromise(aClass, id)));
    }

    @Override
    public Homo<TestRsp> testProto(Integer podId, ParameterMsg parameterMsg, TestReq testReq) {
        log.info("UserService testProto podId {} parameterMsg {} testReq {}", podId, parameterMsg, testReq);
        TestRsp testRsp = TestRsp.newBuilder().setCode(1).setMsg("456").build();
        return Homo.result(testRsp);
    }

    @Override
    public Homo<String> testJson(JSONObject param, JSONObject header) {
        log.info("UserService testJson param {} header {}", param, header);
        JSONObject ret = new JSONObject();
        ret.put("ret", "123");
        return Homo.result(ret.toJSONString());
    }
}
