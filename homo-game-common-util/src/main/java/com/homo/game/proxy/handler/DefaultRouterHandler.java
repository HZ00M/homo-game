package com.homo.game.proxy.handler;

import com.google.protobuf.ByteString;
import com.homo.core.facade.service.ServiceStateMgr;
import com.homo.core.rpc.base.serial.ByteRpcContent;
import com.homo.core.rpc.client.ExchangeHostName;
import com.homo.core.rpc.client.RpcClientMgr;
import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.serial.FSTSerializationProcessor;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import io.homo.proto.client.ParameterMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;

import java.util.List;

@Component
@Slf4j
public class DefaultRouterHandler implements RouterHandler {
    @Autowired
    RpcClientMgr rpcClientMgr;

    @Override
    public Integer order() {
        return 10;
    }

    @Autowired
    ServiceStateMgr serviceStateMgr;
    private FSTSerializationProcessor serializationProcessor = new FSTSerializationProcessor();

    @Override
    public Homo<Object> handler(HandlerContext context) {
        ClientRouterMsg routerMsg = context.getRouterMsg();
        ClientRouterHeader header = context.getHeader();
        String userId = routerMsg.getUserId();
        String msgId = routerMsg.getMsgId();
        String srcService = routerMsg.getSrcService();
        List<ByteString> msgContentList = routerMsg.getMsgContentList();
        String entityType = context.getParam(RouterHandler.PARAM_ENTITY_TYPE, String.class);
        if (!StringUtils.isEmpty(entityType)) {
            srcService = entityType;

        }
        String finalSrcService = srcService;
        return serviceStateMgr.getServiceInfo(srcService)
                .nextDo(serviceInfo -> {
                    log.info("router userId {} srcService {} msgId {}", userId, finalSrcService, msgId);
                    Integer podId = -1;//proxy选择
                    ParameterMsg parameterMsg = ParameterMsg.newBuilder().setUserId(userId).build();//构建用户基本信息
                    return ExchangeHostName.exchange(serviceInfo, podId, parameterMsg)
                            .nextDo(realHostName -> {
                                byte[][] data = new byte[msgContentList.size() + 2][];
                                data[0] = serializationProcessor.writeByte(podId);
                                data[1] = parameterMsg.toByteArray();
                                for (ByteString bytes : msgContentList) {
                                    data[msgContentList.indexOf(bytes) + 2] = bytes.toByteArray();
                                }
                                ByteRpcContent rpcContent = ByteRpcContent.builder().data(data).build();
                                return rpcClientMgr.getGrpcAgentClient(realHostName)
                                        .rpcCall(msgId, rpcContent)
                                        .nextDo(ret -> {
                                            Tuple2<String, ByteRpcContent> contentTuple2 = (Tuple2<String, ByteRpcContent>) ret;
                                            context.promiseResult(contentTuple2);
                                            log.info("DefaultRouterHandler handler success userId {} msgId {}", userId, msgId);
                                            return context.handler(context);
                                        })
                                        .catchError(throwable -> {
                                            log.error("DefaultRouterHandler handler error userId {} msgId {} throwable {}", userId, msgId, throwable);
                                            context.error((Throwable) throwable);
                                        });
                            });
                });

    }
}
