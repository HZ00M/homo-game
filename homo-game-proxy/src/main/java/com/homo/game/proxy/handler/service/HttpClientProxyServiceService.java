package com.homo.game.proxy.handler.service;

import com.google.protobuf.ByteString;
import com.homo.core.rpc.base.serial.ByteRpcContent;
import com.homo.game.proxy.handler.RouterHandler;
import com.homo.game.proxy.handler.RouterHandlerManger;
import com.homo.core.rpc.base.service.BaseService;
import com.homo.core.rpc.client.RpcClientMgr;
import com.homo.core.utils.rector.Homo;
import com.homo.game.proxy.proxy.facade.ClientJsonRouterMsg;
import com.homo.game.proxy.proxy.facade.IHttpClientProxyService;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;

import java.util.List;

@Component
@Slf4j
public class HttpClientProxyServiceService extends BaseService implements IHttpClientProxyService {
    @Autowired
    RpcClientMgr rpcClientMgr;
    @Autowired
    RouterHandlerManger handlerMgr;

    @Override
    public void preServerInit() {
        log.info("HttpClientProxy preServerInit call");
    }

    @Override
    public Homo<Msg> clientMsgProxy(ClientRouterMsg routerMsg, ClientRouterHeader header) {
        String msgId = routerMsg.getMsgId();
        String srcService = routerMsg.getSrcService();
        String userId = routerMsg.getUserId();
        String token = routerMsg.getToken();

        log.info("clientMsgProxy msgId {} srcService {} userId {} token {} ",
                msgId, srcService, userId, token);

        return Homo.warp(msgHomoSink -> {
            handlerMgr.create(msgHomoSink, "checkParamMsgHandler", "checkWhiteListHandler", "checkUserNumberHandler",
                            "authTokenHandler", "defaultRouterHandler")
                    .header(header)
                    .router(routerMsg)
                    .sort()
                    .process();
        }).nextDo(ret -> {
            Tuple2<String, ByteRpcContent> tuple = (Tuple2<String, ByteRpcContent>) ret;
            String retMsgId = tuple.getT1();
            byte[][] data = tuple.getT2().getData();
            Msg.Builder gateMsgResp = Msg.newBuilder();
            Msg msg = gateMsgResp.setMsgId(tuple.getT1()).setMsgContent(ByteString.copyFrom(data[0])).build();
            log.info("processRouterMsg success userId {} msgId {}", userId, msgId);
            return Homo.result(msg);
        });
    }


    @Override
    public Homo<String> clientJsonMsgProxy(ClientJsonRouterMsg routerMsg) {
        String serviceName = routerMsg.getServiceName();
        Integer podIndex = routerMsg.getPodIndex();
        String msgId = routerMsg.getMsgId();
        log.info("clientJsonMsgProxy serviceName {} podIndex {} msgId {}", serviceName, podIndex, msgId);
        return Homo.warp(msgHomoSink -> {
            handlerMgr.create(msgHomoSink, "jsonRouterHandler")
                    .setParam(RouterHandler.PARAM_SRC_SERVICE, serviceName)
                    .setParam(RouterHandler.PARAM_POD_ID, podIndex)
                    .setParam(RouterHandler.PARAM_MSG_ID, msgId)
                    .setParam(RouterHandler.PARAM_MSG, routerMsg.getMsgContent())
                    .sort()
                    .process();
        });
    }
}
