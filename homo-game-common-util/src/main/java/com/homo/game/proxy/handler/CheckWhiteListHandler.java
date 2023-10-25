package com.homo.game.proxy.handler;

import com.homo.game.proxy.config.ProxyHandlerProperties;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.core.utils.rector.Homo;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Component
public class CheckWhiteListHandler implements RouterHandler {

    @Autowired
    ProxyHandlerProperties proxyHandlerProperties;
    @Override
    public Integer order() {
        return 1;
    }

    @Override
    public Homo<Void> handler(HandlerContext context) {
        String msgId = context.getParam(RouterHandler.PARAM_MSG_ID,String.class);
        String srcService = context.getParam(RouterHandler.PARAM_SRC_SERVICE,String.class);
        String appId = context.getParam(RouterHandler.PARAM_APP_ID,String.class);
        String channelId = context.getParam(RouterHandler.PARAM_CHANNEL_ID,String.class);
        String userId = context.getParam(RouterHandler.PARAM_USER_ID,String.class);
        String token = context.getParam(RouterHandler.PARAM_TOKEN,String.class);
        if (!proxyHandlerProperties.serverEnable && !proxyHandlerProperties.userWhiteList.contains(userId)){
            Tuple2<Boolean, String> tuples = Tuples.of(false, "CheckWhiteListHandler check fail");
            context.success(tuples);
            return Homo.resultVoid();
        }else {
            return context.handler(context);
        }
    }
}
