package com.homo.game.proxy.handler;

import com.homo.core.utils.rector.Homo;

/**
 * 用户登陆流程 1、检查token 2、检查白名单 3、检查激活码
 */
public interface RouterHandler {
    String PARAM_USER_ID = "USER_ID";
    String PARAM_APP_ID = "APP_ID";
    String PARAM_CHANNEL_ID = "CHANNEL_ID";
    String PARAM_TOKEN = "TOKEN";
    String PARAM_SRC_SERVICE = "SRC_SERVICE";
    String PARAM_MSG_ID = "MSG_ID";
    String PARAM_MSG = "MSG";
    String PARAM_ENTITY_TYPE = "ENTITY_TYPE";
    String PARAM_POD_ID = "POD_ID";
    String HEADER_APP_VERSION = "APP_VERSION";
    String HEADER_RES_VERSION = "RES_VERSION";
    String HEADER_AD_ID = "AD_ID";
    String PARAM_SYNC_INFO = "SYNC_INFO";
    String PARAMETER_MSG = "PARAM";
    default String name() {
        return this.getClass().getSimpleName();
    }

    Homo<Object> handler(HandlerContext context);

    default Integer order() {
        return 0;
    }
}
