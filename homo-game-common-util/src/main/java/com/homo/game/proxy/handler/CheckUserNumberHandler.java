package com.homo.game.proxy.handler;

import com.homo.game.proxy.config.ProxyHandlerProperties;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.core.facade.module.ServerInfo;
import com.homo.core.facade.module.ServiceModule;
import com.homo.core.storage.ByteStorage;
import com.homo.core.utils.rector.Homo;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CheckUserNumberHandler implements ProxyHandler, ServiceModule {
    @Autowired
    ProxyHandlerProperties proxyHandlerProperties;
    @Autowired
    private ByteStorage storage;
    private Long serverPlayerNumber = 0L;

    @Override
    public void init() {
        loadServerPlayerNumber();
    }

    @Override
    public Integer order() {
        return 2;
    }

    private void loadServerPlayerNumber() {
        storage.get("limit", "proxy", "userLimit").consumerValue(bytes -> {
            if (bytes != null) {
                serverPlayerNumber = Long.valueOf(new String(bytes, StandardCharsets.UTF_8));
            }
        }).start();
    }

    public void incrServerPlayerNumber() {
        ServerInfo serverInfo = getServerInfo();
        storage.incr(serverInfo.getAppId(), serverInfo.getRegionId(), "limit", "proxy", "userLimit")
                .consumerValue(curNumber -> serverPlayerNumber = curNumber).start();
    }

    @Override
    public Homo<Void> handler(HandlerContext context) {
        ClientRouterMsg routerMsg = context.getRouterMsg();
        ClientRouterHeader header = context.getHeader();
        if (proxyHandlerProperties.limitEnable && !proxyHandlerProperties.userWhiteList.contains(routerMsg.getUserId())){
            if (serverPlayerNumber >= proxyHandlerProperties.getLimitNum()){
                context.success(Msg.newBuilder().setMsgId(HomoCommonError.user_limit.name()).build());
                return Homo.resultVoid();
            }
            return context.handler(context);
        }else {
            return context.handler(context);
        }
    }
}