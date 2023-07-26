package com.homo.game.proxy.handler;

import com.homo.core.utils.rector.Homo;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.game.proxy.util.ProxyCheckParamUtils;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;
import io.homo.proto.client.ParameterMsg;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class CheckParamMsgHandler implements ProxyHandler{
    static String PARAMETER_MSG = "PARAM";
    @Override
    public Integer order() {
        return 8;
    }
    @Override
    public Homo<Void> handler(HandlerContext context) {
        ClientRouterMsg routerMsg = context.getRouterMsg();
        ClientRouterHeader header = context.getHeader();
        String msgId = routerMsg.getMsgId();
        String srcService = routerMsg.getSrcService();
        String userId = routerMsg.getUserId();
        String token = routerMsg.getToken();
        String channelId = routerMsg.getChannelId();
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
