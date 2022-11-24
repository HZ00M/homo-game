package com.homo.game.activity.core.exception;

import com.homo.core.utils.exception.HomoError;
import com.homo.core.utils.exception.HomoThrowable;


public enum HomoActivityError implements HomoThrowable {
    defaultError(201, "activity activity error %s"),
    initParams(202,"node initParams error name %s"),
    paramCastError(203,"paramCastError  fieldName %s fieldType %s configType %s"),
    initPubAndAsk(204,"node initPubAndAsk error name %s"),
    initComponent(205,"node initComponent error hostNode %s component %s")
    ;

    private int code;
    private String message;
    static {
        for (HomoActivityError homoError : HomoActivityError.values()) {
            HomoError.appendError(homoError.code,homoError.message);
        }
    }

    HomoActivityError(int code, String message) {
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
