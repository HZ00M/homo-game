package com.homo.game.proxy.handler;

import com.homo.game.proxy.config.ProxyHandlerProperties;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.core.utils.rector.Homo;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CheckWhiteListHandler implements ProxyHandler {

    @Autowired
    ProxyHandlerProperties proxyHandlerProperties;
    @Override
    public Integer order() {
        return 1;
    }

    @Override
    public Homo<Void> handler(HandlerContext context) {
        ClientRouterMsg routerMsg = context.getRouterMsg();
        String userId = routerMsg.getUserId();
        ClientRouterHeader header = context.getHeader();
        if (!proxyHandlerProperties.serverEnable && !proxyHandlerProperties.userWhiteList.contains(userId)){
            context.success(Msg.newBuilder().setMsgId(HomoCommonError.token_error.name()).build());
            return Homo.resultVoid();
        }else {
            return context.handler(context);
        }
    }
}
