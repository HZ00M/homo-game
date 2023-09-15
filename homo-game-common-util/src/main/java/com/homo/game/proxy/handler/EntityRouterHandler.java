package com.homo.game.proxy.handler;

import com.google.protobuf.ByteString;
import com.homo.core.utils.serial.FSTSerializationProcessor;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.core.facade.rpc.RpcAgentClient;
import com.homo.core.facade.service.ServiceStateMgr;
import com.homo.core.rpc.base.serial.ByteRpcContent;
import com.homo.core.rpc.base.utils.ServiceUtil;
import com.homo.core.rpc.client.RpcClientMgr;
import com.homo.core.utils.rector.Homo;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;
import io.homo.proto.client.ParameterMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;

import java.util.List;

@Component
@Slf4j
public class EntityRouterHandler implements RouterHandler {
    //    static String SERVICE_NAME = "SERVICE_NAME";
    @Autowired
    ServiceStateMgr serviceStateMgr;
    @Autowired
    RpcClientMgr rpcClientMgr;
    private FSTSerializationProcessor serializationProcessor = new FSTSerializationProcessor();
    @Override
    public Integer order() {
        return 9;
    }
    @Override
    public Homo<Void> handler(HandlerContext context) {
        String msgId = context.getParam(RouterHandler.PARAM_MSG_ID,String.class);
        String srcService = context.getParam(RouterHandler.PARAM_SRC_SERVICE,String.class);
        String userId = context.getParam(RouterHandler.PARAM_USER_ID,String.class);
        String entityType = context.getParam(RouterHandler.PARAM_ENTITY_TYPE,String.class);
        ParameterMsg parameterMsg = context.getParam(RouterHandler.PARAMETER_MSG, ParameterMsg.class);
        List<ByteString> msgContentList = context.getParam(RouterHandler.PARAM_MSG,List.class);
        if (!StringUtils.isEmpty(entityType)) {
            return serviceStateMgr.getServiceInfo(entityType)
                    .nextDo(serverInfo -> {
                        if (serverInfo == null) {
                            log.error("checkServiceName getServiceNameByTag is null. uid: {}, entityType: {}", userId, entityType);
                            context.success(Msg.newBuilder().setCode(HomoCommonError.entity_type_not_found.getCode()).setMsgId(HomoCommonError.entity_type_not_found.msgFormat(entityType)).build());
                            return Homo.resultVoid();
                        } else {
                            return serviceStateMgr.getLinkedPod(userId, srcService)
                                    .nextDo(podIdx->{
                                        if (podIdx == -1){
                                            return serviceStateMgr.choiceBestPod(srcService);
                                        }else {
                                            return Homo.result(podIdx);
                                        }

                                    })
                                    .nextDo(choicePodId->{
                                        String statefulName = ServiceUtil.formatStatefulName(serverInfo.getServiceTag(), choicePodId);
                                        RpcAgentClient agentClient = rpcClientMgr.getGrpcAgentClient(statefulName, true);


                                        byte[][] data = new byte[msgContentList.size() + 2][];
                                        data[0] = serializationProcessor.writeByte(choicePodId);
                                        data[1] = parameterMsg.toByteArray();
                                        for (ByteString bytes : msgContentList) {
                                            data[msgContentList.indexOf(bytes) + 2] = bytes.toByteArray();
                                        }
                                        ByteRpcContent rpcContent = ByteRpcContent.builder().data(data).build();
                                        return agentClient.rpcCall(msgId, rpcContent)
                                                .nextDo(ret->{
                                                    Tuple2<String,ByteRpcContent> contentTuple2 = (Tuple2<String, ByteRpcContent>) ret;
                                                    String retMsgId = contentTuple2.getT1();
                                                    ByteRpcContent content = contentTuple2.getT2();
                                                    byte[][] retData = content.getData();
                                                    Msg msg = Msg.newBuilder().setMsgId(retMsgId).setMsgContent(ByteString.copyFrom(retData[0])).build();
                                                    context.success(msg);
                                                    return Homo.resultVoid();
                                                });
                                    });
                        }
                    });
        } else {
            return context.handler(context);
        }
    }
}
