package com.homo.entity.test.facade.service;

import com.homo.core.facade.rpc.RpcType;
import com.homo.core.facade.service.ServiceExport;
import com.homo.core.utils.rector.Homo;
import io.homo.proto.client.ParameterMsg;
import io.homo.proto.client.TestReq;
import io.homo.proto.client.TestRsp;

@ServiceExport(tagName = "entity-test-service:11555",driverType = RpcType.grpc,isMainServer = true,isStateful = true)
public interface IUserService {

    Homo<TestRsp> test(Integer podId, ParameterMsg parameterMsg, TestReq testReq);
}
