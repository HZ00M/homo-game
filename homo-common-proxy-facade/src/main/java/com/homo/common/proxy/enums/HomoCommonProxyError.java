package com.homo.common.proxy.enums;

import com.homo.core.utils.exception.HomoError;
import com.homo.core.utils.exception.HomoThrowable;

public enum HomoCommonProxyError implements HomoThrowable {
    success(200, "success"),
    flow_limit_error(301, "flow limit %s"),
    common_system_error(302, "common_system_error "),
    token_error(303, "token error %s"),
    no_forward_url(304, "can not find forwardKey %s url"),
    param_miss(305, "param miss %s"),
    sign_error(306, "sign error "),
    ;

    private int code;
    private String message;

    static {
        for (HomoCommonProxyError homoError : HomoCommonProxyError.values()) {
            HomoError.appendError(homoError.code, homoError.message);
        }
    }

    HomoCommonProxyError(int code, String message) {
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
