package com.homo.common.proxy.facade;

import com.alibaba.fastjson.JSONObject;
import com.homo.common.proxy.dto.HeaderParam;
import com.homo.common.proxy.dto.ProxyParam;
import com.homo.core.facade.rpc.RpcType;
import com.homo.core.facade.service.ServiceExport;
import com.homo.core.utils.rector.Homo;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.PbResponseMsg;

@ServiceExport(tagName = "common-http-proxy:33306", driverType = RpcType.http, isStateful = false, isMainServer = true)
public interface ICommonHttpClientProxy {
    /**
     * 统一处理客户端json类型消息,根据客户端的消息id，转发到对应的服务上，检查token
     * @param headerParam        消息
     * @param proxyParam  消息头的msg
     */
    Homo<String> clientJsonMsgCheckToken(ProxyParam proxyParam,HeaderParam headerParam);

    /**
     * 统一处理客户端json类型消息,根据客户端的消息id，转发到对应的服务上，检查sign
     * @param headerParam        消息
     * @param proxyParam  消息头的msg
     */
    Homo<String> clientJsonMsgCheckSign(ProxyParam proxyParam,HeaderParam headerParam);

    /**
     *  统一处理客户端PB类型消息，根据客户端的消息id，转发到对应的服务上，检查token
     * @param clientRouterMsg
     * @return
     */
    Homo<PbResponseMsg> clientPbMsgCheckToken(ClientRouterMsg clientRouterMsg);

    /**
     *  统一处理客户端PB类型消息，根据客户端的消息id，转发到对应的服务上，检查sign
     * @param clientRouterMsg
     * @return
     */
    Homo<PbResponseMsg> clientPbMsgCheckSign(ClientRouterMsg clientRouterMsg);

    /**
     * 反向代理http请求，用于外部公网访问
     * @param headerJson
     * @param requestJson
     * @return
     */
     Homo<String> httpForward(JSONObject requestJson,JSONObject headerJson);

//    /**
//     * 反向代理http请求，转发内部k8s服务，用于内部私网访问
//     * @param headerJson
//     * @param requestJson0
//     * @return
//     */
//     Homo<String> innerHttpForward(JSONObject headerJson,JSONObject requestJson0);

}
