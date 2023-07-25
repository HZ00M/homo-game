package com.homo.game.proxy.test.facade;

import com.alibaba.fastjson.JSONObject;
import com.homo.core.facade.rpc.RpcType;
import com.homo.core.facade.service.ServiceExport;
import com.homo.core.utils.rector.Homo;
import io.homo.proto.client.HttpTestReq;
import io.homo.proto.client.HttpTestRsp;

@ServiceExport(tagName = "rpc-test-service:33334", isStateful = false, driverType = RpcType.grpc, isMainServer = false)
public interface IRpcTestService {

    Homo<String> jsonPostTest1(JSONObject params, JSONObject header);

    Homo<HttpTestRsp> protoPostTest1(HttpTestReq req);
}
