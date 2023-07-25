package com.homo.game.proxy.handler;

import com.homo.core.utils.rector.Homo;

/**
 * 用户登陆流程 1、检查token 2、检查白名单 3、检查激活码
 */
public interface ProxyHandler {

    default String name() {
        return this.getClass().getSimpleName();
    }

    Homo<Void> handler(HandlerContext context);

    default Integer order() {
        return 0;
    }
}
