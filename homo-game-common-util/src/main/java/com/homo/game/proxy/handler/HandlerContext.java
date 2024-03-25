package com.homo.game.proxy.handler;

import com.google.protobuf.ByteString;
import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.rector.HomoSink;

import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class HandlerContext implements RouterHandler {
    private ClientRouterHeader header;
    private ClientRouterMsg routerMsg;
    private List<RouterHandler> filters;
    private int index;
    private HomoSink sink;
    private Map<String, Object> contextMap = new HashMap<>();
    private Object result;

    public HandlerContext(HomoSink sink, List<RouterHandler> filters) {
        this.sink = sink;
        this.filters = filters;
    }

    public HandlerContext header(ClientRouterHeader header) {
        this.header = header;
        String adId = header.getHeadersMap().get(RouterHandler.HEADER_AD_ID);
        String appVersion = header.getHeadersMap().get(RouterHandler.HEADER_APP_VERSION);
        String resVersion = header.getHeadersMap().get(RouterHandler.HEADER_RES_VERSION);
        this.setParam(RouterHandler.HEADER_AD_ID, adId);
        this.setParam(RouterHandler.HEADER_APP_VERSION, appVersion);
        this.setParam(RouterHandler.HEADER_RES_VERSION, resVersion);
        return this;
    }

    public HandlerContext router(ClientRouterMsg routerMsg) {
        this.routerMsg = routerMsg;
        String msgId = routerMsg.getMsgId();
        String srcService = routerMsg.getSrcService();
        String userId = routerMsg.getUserId();
        String token = routerMsg.getToken();
        String channelId = routerMsg.getChannelId();
        String entityType = routerMsg.getEntityType();
        List<ByteString> msgContentList = routerMsg.getMsgContentList();

        this.setParam(RouterHandler.PARAM_MSG_ID, msgId);
        this.setParam(RouterHandler.PARAM_SRC_SERVICE, srcService);
        this.setParam(RouterHandler.PARAM_USER_ID, userId);
        this.setParam(RouterHandler.PARAM_TOKEN, token);
        this.setParam(RouterHandler.PARAM_CHANNEL_ID, channelId);
        this.setParam(RouterHandler.PARAM_ENTITY_TYPE, entityType);
        this.setParam(RouterHandler.PARAM_MSG, msgContentList);
        return this;
    }


    public HandlerContext sort() {
        this.filters.sort(Comparator.comparing(RouterHandler::order));
        return this;
    }

    public <T> T getParam(String key, Class<T> type) {
        return (T) this.contextMap.get(key);
    }

    public HandlerContext setParam(String key, Object value) {
        this.contextMap.put(key, value);
        return this;
    }

    public void promiseResult(Object obj) {
        this.result = obj;
    }

    public void success(Object obj) {
        sink.success(obj);
    }

    public void error(Throwable throwable) {
        sink.error(throwable);
    }

    public void process() {
        this.handler(this).consumerValue(ret->{
            if (sink!= null){
                sink.success(ret);
            }
        }).start();
    }

    @Override
    public Homo<Object> handler(HandlerContext context) {
        if (result != null) {
            return Homo.result(result);
        }
        if (index == filters.size()) {
            log.info("FilterChain Filter done");
            return Homo.result(result);
        }
        RouterHandler handler = filters.get(index);
        index++;
        try {
            log.info("HandlerContext index {} handler {}", index, handler.name());
            return handler.handler(context);
        } catch (Exception e) {
            log.error("FilterChain error index {} filter {} e", index, handler.name(), e);
            return Homo.error(e);
        }
    }
}
