package com.homo.game.proxy.handler;

import com.homo.core.utils.rector.Homo;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.game.proxy.util.ProxyCheckParamUtils;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;
import io.homo.proto.client.ParameterMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CheckParamMsgHandler implements RouterHandler {

    @Override
    public Integer order() {
        return 8;
    }
    @Override
    public Homo<Void> handler(HandlerContext context) {
        String msgId = context.getParam(RouterHandler.PARAM_MSG_ID,String.class);
        String srcService = context.getParam(RouterHandler.PARAM_SRC_SERVICE,String.class);
        String appId = context.getParam(RouterHandler.PARAM_APP_ID,String.class);
        String channelId = context.getParam(RouterHandler.PARAM_CHANNEL_ID,String.class);
        String userId = context.getParam(RouterHandler.PARAM_USER_ID,String.class);
        String token = context.getParam(RouterHandler.PARAM_TOKEN,String.class);
        boolean isNullOrEmpty = ProxyCheckParamUtils.checkIsNullOrEmpty(msgId, srcService, userId, token, channelId);
        if (isNullOrEmpty){
            log.error("CheckParamMsgHandler param error msgId {} srcService {} userId {} token {} channelId {}",
                    msgId,srcService,userId,token,channelId);
            context.success(Msg.newBuilder().setCode(HomoCommonError.param_miss.getCode()).setCodeDesc(HomoCommonError.param_miss.message()).build());
            return Homo.resultVoid();
        }
        ParameterMsg parameterMsg = ParameterMsg.newBuilder().setUserId(userId).setChannelId(channelId).build();
        context.setParam(PARAMETER_MSG,parameterMsg);
        return context.handler(context);
    }
}
