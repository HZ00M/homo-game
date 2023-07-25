package com.homo.game.proxy.handler.service;

import com.homo.game.proxy.handler.ProxyHandler;
import com.homo.game.proxy.handler.HandlerManger;
import com.homo.core.rpc.base.service.BaseService;
import com.homo.core.rpc.client.RpcClientMgr;
import com.homo.core.utils.rector.Homo;
import com.homo.game.proxy.proxy.facade.ClientJsonRouterMsg;
import com.homo.game.proxy.proxy.facade.ClientProxyKey;
import com.homo.game.proxy.proxy.facade.IHttpClientProxy;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log4j2
public class HttpClientProxyService extends BaseService implements IHttpClientProxy {
    @Autowired
    RpcClientMgr rpcClientMgr;
    @Autowired
    List<ProxyHandler> filterList;
    @Autowired
    HandlerManger handlerMgr;

    @Override
    public void postInit() {
        log.info("HttpClientProxy postInit call");
    }

    @Override
    public Homo<Msg> clientMsgProxy(ClientRouterMsg routerMsg, ClientRouterHeader header) {
        String msgId = routerMsg.getMsgId();
        String srcService = routerMsg.getSrcService();
        String userId = routerMsg.getUserId();
        String token = routerMsg.getToken();
        String adId = header.getHeadersMap().get(ClientProxyKey.adId);
        String appVersion = header.getHeadersMap().get(ClientProxyKey.appVersion);
        String resVersion = header.getHeadersMap().get(ClientProxyKey.resVersion);
        log.info("clientMsgProxy msgId {} srcService {} userId {} token {} adId {} appVersion {} resVersion {}",
                msgId, srcService, userId, token, adId, appVersion, resVersion);

        return Homo.warp(msgHomoSink -> {
            handlerMgr.create(msgHomoSink, "authTokenHandler", "checkWhiteListHandler", "checkUserNumberHandler",
                            "fillParamMsgHandler","entityRouterHandler","routerHandler")
                    .header(header)
                    .router(routerMsg)
                    .sort()
                    .process();
        });
    }


    @Override
    public Homo<String> clientJsonMsgProxy(ClientJsonRouterMsg routerMsg) {
        String serviceName = routerMsg.getServiceName();
        Integer podIndex = routerMsg.getPodIndex();
        String msgId = routerMsg.getMsgId();
        log.info("clientJsonMsgProxy serviceName {} podIndex {} msgId {}",serviceName,podIndex,msgId);
        return Homo.warp(msgHomoSink -> {
            handlerMgr.create(msgHomoSink, "jsonRouterHandler")
                    .setParam("jsonParam",routerMsg)
                    .sort()
                    .process();
        });
    }
}
