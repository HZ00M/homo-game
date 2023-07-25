package com.homo.game.proxy.handler;

import com.google.protobuf.ByteString;
import com.homo.game.proxy.enums.HomoCommonError;
import com.homo.core.facade.rpc.RpcAgentClient;
import com.homo.core.facade.service.ServiceStateMgr;
import com.homo.core.rpc.base.serial.ByteRpcContent;
import com.homo.core.rpc.base.utils.ServiceUtil;
import com.homo.core.rpc.client.RpcClientMgr;
import com.homo.core.utils.exception.HomoError;
import com.homo.core.utils.exception.HomoException;
import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.serial.HomoSerializationProcessor;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.Msg;
import io.homo.proto.client.ParameterMsg;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;

import java.util.List;

@Component
@Log4j2
public class EntityRouterHandler implements ProxyHandler {
    //    static String SERVICE_NAME = "SERVICE_NAME";
    @Autowired
    ServiceStateMgr serviceStateMgr;
    @Autowired
    RpcClientMgr rpcClientMgr;
    @Autowired
    HomoSerializationProcessor homoSerializationProcessor;
    @Override
    public Integer order() {
        return 9;
    }
    @Override
    public Homo<Void> handler(HandlerContext context) {
        ClientRouterMsg routerMsg = context.getRouterMsg();
        ClientRouterHeader header = context.getHeader();
        String userId = routerMsg.getUserId();
        String msgId = routerMsg.getMsgId();
        List<ByteString> msgContentList = routerMsg.getMsgContentList();
        String entityType = routerMsg.getEntityType();
        String srcService = routerMsg.getSrcService();
        if (!StringUtils.isEmpty(entityType)) {
            return serviceStateMgr.getServiceInfo(entityType)
                    .nextDo(serverInfo -> {
                        if (serverInfo == null) {
                            log.error("checkServiceName getServiceNameByTag is null. uid: {}, entityType: {}", userId, entityType);
                            context.success(Msg.newBuilder().setCode(HomoCommonError.entity_type_not_found.getCode()).setMsgId(HomoCommonError.entity_type_not_found.msgFormat(entityType)).build());
                            return Homo.resultVoid();
                        } else {
                            return serviceStateMgr.getLinkedPod(userId, srcService)
                                    .nextDo(pod -> {
                                        if (pod != null) {
                                            String statefulName = ServiceUtil.formatStatefulName(serverInfo.getServiceTag(), pod);
                                            RpcAgentClient agentClient = rpcClientMgr.getGrpcAgentClient(statefulName, true);
                                            ParameterMsg parameterMsg = context.getParam(FillParamMsgHandler.PARAMETER_MSG, ParameterMsg.class);

                                            byte[][] data = new byte[msgContentList.size() + 2][];
                                            data[0] = homoSerializationProcessor.writeByte(pod);
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
                                        }else {
                                            HomoException exception = HomoError.throwError(HomoCommonError.entity_pod_not_found.getCode(),userId);
                                            context.error(exception);
                                            return Homo.resultVoid();
                                        }
                                    });
                        }
                    });
        } else {
            return context.handler(context);
        }
    }
}
