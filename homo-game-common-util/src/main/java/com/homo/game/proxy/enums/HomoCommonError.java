package com.homo.game.proxy.enums;

import com.homo.core.utils.exception.HomoError;
import com.homo.core.utils.exception.HomoThrowable;

public enum HomoCommonError implements HomoThrowable {
    success(200, "success"),
    flow_limit_error(301, "flow limit %s"),
    common_system_error(302, "common_system_error "),
    token_error(303, "token error %s"),
    no_forward_url(304, "can not find forwardKey %s url,check config homo.common.httpForward.url.map"),
    param_miss(305, "param miss %s"),
    sign_error(306, "sign error "),
    user_limit(307,"user limit %s"),
    entity_type_not_found(308,"entity type not found %s"),
    entity_pod_not_found(309,"entity pod not fount %s"),
    ;

    private int code;
    private String message;

    static {
        for (HomoCommonError homoError : HomoCommonError.values()) {
            HomoError.appendError(homoError.code, homoError.message);
        }
    }

    HomoCommonError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
