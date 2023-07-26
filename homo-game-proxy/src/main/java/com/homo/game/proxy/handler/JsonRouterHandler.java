package com.homo.game.proxy.handler;

import com.homo.core.facade.rpc.RpcAgentClient;
import com.homo.core.facade.service.ServiceStateMgr;
import com.homo.core.rpc.base.serial.JsonRpcContent;
import com.homo.core.rpc.base.utils.ServiceUtil;
import com.homo.core.rpc.client.RpcClientMgr;
import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.serial.HomoSerializationProcessor;
import com.homo.game.proxy.proxy.facade.ClientJsonRouterMsg;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;

@Component
@Log4j2
public class JsonRouterHandler implements ProxyHandler {
    @Autowired
    ServiceStateMgr serviceStateMgr;
    @Autowired
    RpcClientMgr rpcClientMgr;
    @Autowired
    HomoSerializationProcessor homoSerializationProcessor;
    public static String JSON_PARAM_KEY = "jsonParam";
    @Override
    public Homo<Void> handler(HandlerContext context) {
        ClientJsonRouterMsg routerMsg = context.getParam(JSON_PARAM_KEY, ClientJsonRouterMsg.class);
        String serviceName = routerMsg.getServiceName();
        String msgId = routerMsg.getMsgId();
        String msgContent = routerMsg.getMsgContent();
        Integer podIndex = routerMsg.getPodIndex();
        RpcAgentClient grpcAgentClient;
        if (podIndex != null) {
            //指定pod为有状态服务器
            String formatStatefulName = ServiceUtil.formatStatefulName(serviceName, podIndex);
            grpcAgentClient = rpcClientMgr.getGrpcAgentClient(formatStatefulName, true);
        }else {
            grpcAgentClient = rpcClientMgr.getGrpcServerlessAgentClient(serviceName);
        }
        JsonRpcContent rpcContent = JsonRpcContent.builder().data(msgContent).build();
        return grpcAgentClient.rpcCall(msgId, rpcContent)
                .nextDo(ret -> {
                    Tuple2<String, JsonRpcContent> tuple2 = (Tuple2<String, JsonRpcContent>) ret;
                    String retMsgId = tuple2.getT1();
                    JsonRpcContent retJsonContent = tuple2.getT2();
                    context.success(retJsonContent.getData());
                    return Homo.resultVoid();
                });
    }
}
