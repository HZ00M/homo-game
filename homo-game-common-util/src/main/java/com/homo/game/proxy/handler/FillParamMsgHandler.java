package com.homo.game.proxy.handler;

import com.homo.core.utils.rector.Homo;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.ParameterMsg;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class FillParamMsgHandler implements ProxyHandler{
    static String PARAMETER_MSG = "PARAM";
    @Override
    public Integer order() {
        return 8;
    }
    @Override
    public Homo<Void> handler(HandlerContext context) {
        ClientRouterMsg routerMsg = context.getRouterMsg();
        ClientRouterHeader header = context.getHeader();
        String userId = routerMsg.getUserId();
        String channelId = routerMsg.getChannelId();
        ParameterMsg parameterMsg = ParameterMsg.newBuilder().setUserId(userId).setChannelId(channelId).build();
        context.setParam(PARAMETER_MSG,parameterMsg);
        return context.handler(context);
    }
}
