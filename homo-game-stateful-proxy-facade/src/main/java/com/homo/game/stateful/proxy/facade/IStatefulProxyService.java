package com.homo.game.stateful.proxy.facade;

import com.homo.core.facade.rpc.RpcType;
import com.homo.core.facade.service.ServiceExport;
import com.homo.core.utils.rector.Homo;
import io.homo.proto.client.*;

@ServiceExport(tagName = "stateful-proxy:31666",isMainServer = true,isStateful = true,driverType = RpcType.grpc)
public interface IStatefulProxyService {
//    Homo<LoginMsgResp> login(LoginMsgReq req);
//    Homo<LoginAndSyncResp> reconnect(LoginAndSyncReq req);

    /**
     * 将内部消息通过Proxy发往客户端
     * @param podId
     * @param req
     * @return
     */
    Homo<ToClientResp> sendToClient(Integer podId, ToClientReq req);

    /**
     * 将内部消息通过Proxy发往客户端，带回调
     * @param podId
     * @param req
     * @return
     */
    Homo<ToClientResp> sendToClientComplete(Integer podId, ToClientReq req);


    /**
     * 将消息发往与该proxy相连的所有客户端
     * @param podId
     * @param req
     * @return
     */
    Homo<ToClientResp> sendToAllClient(Integer podId,ToClientReq req);

    /**
     * 断开与该proxy相连的某个客户端
     * @param podId
     * @param uid
     * @return
     */
    Homo<Boolean> kickLocalClient(Integer podId,String uid);

    /**
     * 断线重连后把未收到的消息同步到新连接的节点上
     * @param podId
     * @param req
     * @return
     */
    Homo<TransferCacheResp> transferCache(Integer podId, TransferCacheReq req);

    Homo<ToClientResp> forwardMsg(Integer podId,ToClientReq req);
}
