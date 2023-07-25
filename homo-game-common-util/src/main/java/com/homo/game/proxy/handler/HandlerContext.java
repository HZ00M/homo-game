package com.homo.game.proxy.handler;

import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.rector.HomoSink;
import io.homo.proto.client.ClientRouterHeader;
import io.homo.proto.client.ClientRouterMsg;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Log4j2
public class HandlerContext implements ProxyHandler {
    private ClientRouterHeader header;
    private ClientRouterMsg routerMsg;
    private List<ProxyHandler> filters;
    private int index;
    private HomoSink sink;
    private Map<String, Object> contextMap = new HashMap<>();

    public HandlerContext(HomoSink sink, List<ProxyHandler> filters) {
        this.sink = sink;
        this.filters = filters;
    }

    public HandlerContext header(ClientRouterHeader header) {
        this.header = header;
        return this;
    }

    public HandlerContext router(ClientRouterMsg routerMsg) {
        this.routerMsg = routerMsg;
        return this;
    }

    public HandlerContext sort() {
        this.filters.sort(Comparator.comparing(ProxyHandler::order));
        return this;
    }

    public HandlerContext setParam(String key, Object value) {
        this.contextMap.put(key, value);
        return this;
    }

    public <T> T getParam(String key, Class<T> type) {
        return (T) this.contextMap.get(key);
    }

    public void success(Object obj) {
        sink.success(obj);
    }

    public void error(Throwable throwable) {
        sink.error(throwable);
    }

    public void process() {
        this.handler(this).start();
    }

    @Override
    public Homo<Void> handler(HandlerContext context) {
        if (index == filters.size()) {
            log.info("FilterChain Filter done");
            return Homo.resultVoid();
        }
        ProxyHandler handler = filters.get(index);
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
