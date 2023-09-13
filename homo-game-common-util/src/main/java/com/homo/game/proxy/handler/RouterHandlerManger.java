package com.homo.game.proxy.handler;

import com.homo.core.utils.exception.HomoError;
import com.homo.core.utils.rector.HomoSink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class RouterHandlerManger {
    @Autowired
    public Map<String, RouterHandler> handlerMap;

    public HandlerContext create(HomoSink sink, String... handlerName) {
        List<RouterHandler> list = new ArrayList<>();
        for (String filterName : handlerName) {
            RouterHandler routerHandler = handlerMap.get(filterName);
            if (routerHandler == null){
                throw HomoError.throwError(HomoError.defaultError,filterName,"not found");
            }
            list.add(routerHandler);
        }
        HandlerContext context = new HandlerContext(sink, list);
        return context;
    }

}
