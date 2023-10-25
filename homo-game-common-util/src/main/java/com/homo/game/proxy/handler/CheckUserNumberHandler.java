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
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.nio.charset.StandardCharsets;

@Component
public class CheckUserNumberHandler implements RouterHandler, ServiceModule {
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
        String msgId = context.getParam(RouterHandler.PARAM_MSG_ID,String.class);
        String srcService = context.getParam(RouterHandler.PARAM_SRC_SERVICE,String.class);
        String appId = context.getParam(RouterHandler.PARAM_APP_ID,String.class);
        String channelId = context.getParam(RouterHandler.PARAM_CHANNEL_ID,String.class);
        String userId = context.getParam(RouterHandler.PARAM_USER_ID,String.class);
        String token = context.getParam(RouterHandler.PARAM_TOKEN,String.class);
        if (proxyHandlerProperties.limitEnable && !proxyHandlerProperties.userWhiteList.contains(userId)){
            if (serverPlayerNumber >= proxyHandlerProperties.getLimitNum()){
                Tuple2<Boolean, String> tuples = Tuples.of(false, "CheckUserNumberHandler check fail");
                context.success(tuples);
                return Homo.resultVoid();
            }
            return context.handler(context);
        }else {
            return context.handler(context);
        }
    }
}
