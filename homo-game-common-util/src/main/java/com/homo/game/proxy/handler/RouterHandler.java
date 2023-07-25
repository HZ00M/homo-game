package com.homo.game.proxy.handler;

import com.google.protobuf.ByteString;
import com.homo.core.facade.rpc.RpcAgentClient;
import com.homo.core.facade.service.ServiceStateMgr;
import com.homo.core.rpc.base.serial.ByteRpcContent;
import com.homo.core.rpc.base.utils.ServiceUtil;
import com.homo.core.rpc.client.RpcClientMgr;
import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.serial.HomoSerializationProcessor;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;
import io.homo.proto.client.ParameterMsg;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;

import java.util.List;

@Component
@Log4j2
public class RouterHandler implements ProxyHandler {
    @Override
    public Integer order() {
        return 10;
    }

    @Autowired
    ServiceStateMgr serviceStateMgr;
    @Autowired
    RpcClientMgr rpcClientMgr;
    @Autowired
    HomoSerializationProcessor homoSerializationProcessor;

    @Override
    public Homo<Void> handler(HandlerContext context) {
        ClientRouterMsg routerMsg = context.getRouterMsg();
        ClientRouterHeader header = context.getHeader();
        String userId = routerMsg.getUserId();
        String msgId = routerMsg.getMsgId();
        List<ByteString> msgContentList = routerMsg.getMsgContentList();
        String srcService = routerMsg.getSrcService();
        return serviceStateMgr.getServiceInfo(srcService)
                .nextDo(serviceInfo -> {
                    if (serviceInfo.isStateful > 0){
                        return serviceStateMgr.getLinkedPod(userId,srcService)
                                .nextDo(podIdx->{
                                    if (podIdx == -1){
                                        return serviceStateMgr.choiceBestPod(srcService);
                                    }else {
                                        return Homo.result(podIdx);
                                    }

                                })
                                .nextDo(choicePodId->{
                                    String statefulName = ServiceUtil.formatStatefulName(serviceInfo.getServiceTag(), choicePodId);
                                    RpcAgentClient agentClient = rpcClientMgr.getGrpcAgentClient(statefulName, true);
                                    return Homo.result(agentClient);
                                })
                                ;
                    }else {
                        RpcAgentClient agentClient = rpcClientMgr.getGrpcServerlessAgentClient(serviceInfo.serviceTag);
                        return Homo.result(agentClient);
                    }
                })
                .nextDo(client->{
                    ParameterMsg parameterMsg = context.getParam(FillParamMsgHandler.PARAMETER_MSG, ParameterMsg.class);

                    byte[][] data = new byte[msgContentList.size() + 2][];
                    data[0] = homoSerializationProcessor.writeByte(-1);
                    data[1] = parameterMsg.toByteArray();
                    for (ByteString bytes : msgContentList) {
                        data[msgContentList.indexOf(bytes) + 2] = bytes.toByteArray();
                    }
                    ByteRpcContent rpcContent = ByteRpcContent.builder().data(data).build();
                    return client.rpcCall(msgId, rpcContent)
                            .nextDo(ret -> {
                                Tuple2<String, ByteRpcContent> contentTuple2 = (Tuple2<String, ByteRpcContent>) ret;
                                String retMsgId = contentTuple2.getT1();
                                ByteRpcContent content = contentTuple2.getT2();
                                byte[][] retData = content.getData();
                                Msg msg = Msg.newBuilder().setMsgId(retMsgId).setMsgContent(ByteString.copyFrom(retData[0])).build();
                                context.success(msg);
                                return Homo.resultVoid();
                            });
                })
                ;
    }
}
