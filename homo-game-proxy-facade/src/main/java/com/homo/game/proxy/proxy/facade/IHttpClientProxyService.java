package com.homo.game.proxy.proxy.facade;

import com.homo.core.facade.rpc.RpcType;
import com.homo.core.facade.service.ServiceExport;
import com.homo.core.utils.rector.Homo;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;

/**
 * 客户端http代理 转发消息到内部服务器（可能是有状态服，也可能是无状态服）
 */
@ServiceExport(tagName = "http-client-proxy:31506",driverType = RpcType.http,isStateful = false,isMainServer = true)
public interface IHttpClientProxyService {
    /**
     * 处理客户端消息，解析消息头，然后将消息体转发到对应的服务上
     * Params:
     * ClientRouterMsg – 客户端消息，携带用户id(userId)，校验信息（token）, 路由信息（serviceName）函数名（returnMsgId），和消息体，
     * @param routerMsg
     * @param header
     * @return
     */
    Homo<Msg> clientMsgProxy(ClientRouterMsg routerMsg, ClientRouterHeader header);

//    /**
//     * 处理客户端实体消息，解析消息头，然后将消息体转发到对应的有状态服务上 todo 合并到clientMsgProxy
//     * Params:
//     * ClientEntityRouterMsg – 客户端消息，携带用户id(userId)，校验信息（token）, 实体类型（entityType）函数名（msgId），和消息体，
//     * @param routerMsg
//     * @param header
//     * @return
//     */
//    Homo<Msg> clientEntityMsgProxy(ClientEntityRouterMsg routerMsg,ClientRouterHeader header);

    /**
     * 处理客户端json消息
     * ClientJsonRouterMsg - 目标服务器podIndex, 路由信息（serviceName）
     * @return
     */
    Homo<String> clientJsonMsgProxy(ClientJsonRouterMsg routerMsg);

}
