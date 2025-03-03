package com.homo.game.proxy.handler.handler;

import com.homo.core.facade.rpc.RpcAgentClient;
import com.homo.core.facade.service.ServiceInfo;
import com.homo.core.facade.service.ServiceStateMgr;
import com.homo.core.rpc.base.serial.JsonRpcContent;
import com.homo.core.rpc.base.utils.ServiceUtil;
import com.homo.core.rpc.client.RpcClientMgr;
import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.serial.HomoSerializationProcessor;
import com.homo.game.proxy.handler.HandlerContext;
import com.homo.game.proxy.handler.RouterHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;

@Component
@Slf4j
public class JsonRouterHandler implements RouterHandler {
    @Autowired
    ServiceStateMgr serviceStateMgr;
    @Autowired
    RpcClientMgr rpcClientMgr;
    @Autowired
    HomoSerializationProcessor homoSerializationProcessor;
    @Override
    public Homo<Object> handler(HandlerContext context) {
        String msgContent = context.getParam(RouterHandler.PARAM_MSG,String.class);
        String msgId = context.getParam(RouterHandler.PARAM_MSG_ID,String.class);
        String srcService = context.getParam(RouterHandler.PARAM_SRC_SERVICE,String.class);
        Integer podIndex = context.getParam(RouterHandler.PARAM_POD_ID,Integer.class);
        ServiceInfo serviceInfo = context.getParam(RouterHandler.PARAM_SRC_SERVICE, ServiceInfo.class);
        RpcAgentClient rpcAgent;
//        if (podIndex != null) {
//            //指定pod为有状态服务器
//            String formatStatefulName = ServiceUtil.formatStatefulName(srcService, podIndex);
//            grpcAgentClient = rpcClientMgr.getAgentClient(formatStatefulName, true);
//        }else {
//            grpcAgentClient = rpcClientMgr.getGrpcAgentClient(srcService);
//        }
        rpcAgent = rpcClientMgr.getAgentClient(srcService,serviceInfo);
        JsonRpcContent rpcContent = new JsonRpcContent();
        rpcContent.setParam(msgContent);
        return rpcAgent.rpcCall(msgId, rpcContent)
                .nextDo(ret -> {
                    Tuple2<String, JsonRpcContent> tuple2 = (Tuple2<String, JsonRpcContent>) ret;
                    String retMsgId = tuple2.getT1();
                    JsonRpcContent retJsonContent = tuple2.getT2();
                    context.promiseResult(retJsonContent.getReturn());
                    return context.handler(context);
                });
    }
}
