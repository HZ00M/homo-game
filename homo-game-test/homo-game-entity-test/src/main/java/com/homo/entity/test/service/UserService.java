package com.homo.entity.test.service;

import com.homo.core.rpc.base.service.BaseService;
import com.homo.core.utils.rector.Homo;
import com.homo.entity.test.facade.service.IUserService;
import io.homo.proto.client.ParameterMsg;
import io.homo.proto.client.TestReq;
import io.homo.proto.client.TestRsp;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class UserService extends BaseService implements IUserService {
    @Override
    public Homo<TestRsp> test(Integer podId, ParameterMsg parameterMsg, TestReq testReq) {
        log.info("UserService test podId {} parameterMsg {} testReq {}",podId,parameterMsg,testReq);
        TestRsp testRsp = TestRsp.newBuilder().setCode(1).setMsg("456").build();
        return Homo.result(testRsp);
    }
}
