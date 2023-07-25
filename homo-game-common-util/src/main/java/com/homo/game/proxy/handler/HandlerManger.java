package com.homo.game.proxy.handler;

import com.homo.core.utils.exception.HomoError;
import com.homo.core.utils.rector.HomoSink;
import com.homo.game.proxy.enums.HomoCommonError;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Log4j2
@Component
public class HandlerManger {
    @Autowired
    public Map<String, ProxyHandler> handlerMap;

    public HandlerContext create(HomoSink sink, String... handlerName) {
        List<ProxyHandler> list = new ArrayList<>();
        for (String filterName : handlerName) {
            ProxyHandler proxyHandler = handlerMap.get(filterName);
            if (proxyHandler == null){
                throw HomoError.throwError(HomoError.defaultError,filterName,"not found");
            }
            list.add(proxyHandler);
        }
        HandlerContext context = new HandlerContext(sink, list);
        return context;
    }

}
