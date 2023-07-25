package com.homo.game.proxy.test.facade;

import com.alibaba.fastjson.JSONObject;
import com.homo.core.facade.rpc.RpcType;
import com.homo.core.facade.service.ServiceExport;
import com.homo.core.utils.rector.Homo;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.HttpTestReq;
import io.homo.proto.client.HttpTestRsp;
import io.homo.proto.client.ParameterMsg;

@ServiceExport(tagName = "http-test-service:33333", isStateful = false, driverType = RpcType.http, isMainServer = true)
public interface IHttpTestService {
    Homo<String> jsonGetTest1(JSONObject params, JSONObject header);

    Homo<String> jsonGetTest2(String params, JSONObject header);

    Homo<String> jsonPostTest1(JSONObject params, JSONObject header);

    Homo<String> jsonPostTest2(String params, JSONObject header);

    Homo<String> jsonPostTest3(String params);

    Homo<String> jsonPostTest4(JSONObject param1, JSONObject param2,JSONObject header);

    Homo<String> jsonPostTest5(Integer podIndex, ParameterMsg parameterMsg, String params, JSONObject header);

    Homo<HttpTestRsp> protoPostTest1(HttpTestReq req, ClientRouterHeader header);

    Homo<HttpTestRsp> protoPostTest2(HttpTestReq req);
}
