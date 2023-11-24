package com.homo.game.proxy.handler;

import com.homo.game.proxy.config.ProxyHandlerProperties;
import com.homo.core.utils.rector.Homo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CheckVersionHandler implements RouterHandler {
    static String ios = "IOS";
    static String android = "Android";
    @Autowired
    private ProxyHandlerProperties handlerProperties;

    @Override
    public Integer order() {
        return 1;
    }

    @Override
    public Homo<Object> handler(HandlerContext context) {
        return context.handler(context);
    }
}
