package com.homo.game.login.facade.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Response implements Serializable {

    private Meta meta;
    private Object data;

    public Response() {
        this.meta = new Meta();
    }

    public static Response FAIL_NOT_NULL = new Response(CommonCode.FAIL_NOT_NULL, "");
    public static Response FAIL_SYSTEM = new Response(CommonCode.FAIL_SYSTEM, "");
    public static Response FAIL_GAME = new Response(CommonCode.FAIL_GAME, "");
    public static Response FAIL_EXIST_ACCOUNT = new Response(CommonCode.FAIL_EXIST_ACCOUNT, "");
    public static Response FAIL_EMPTY_ACCOUNT = new Response(CommonCode.FAIL_EMPTY_ACCOUNT, "");
    public static Response FAIL_NO_USER = new Response(CommonCode.FAIL_NO_USER, "");
    public static Response FAIL_ERROR_PASSWORD = new Response(CommonCode.FAIL_ERROR_PASSWORD, "");
    public static Response FAIL_ACCOUNT_BINDED = new Response(CommonCode.FAIL_ACCOUNT_BINDED, "");
    public static Response SUCCEED = successed();

    public Response(CommonCode code, String logId) {
        meta = new Meta(code.code(), code.msg());
    }

    public Response success() {
        this.meta = new Meta();
        this.meta.setErrorCode(0);
        this.meta.setErrorMessage("");
        return this;
    }

    public static Response successed() {
        Response response = new Response();
        return response.success();
    }

    public Response success(int errorCode, String errmsg, Object data) {
        this.meta = new Meta();
        this.meta.setErrorCode(errorCode);
        this.meta.setErrorMessage(errmsg);
        this.data = data;
        return this;
    }

    public Response success(Object data) {
        this.meta = new Meta();
        this.meta.setErrorCode(0);
        this.meta.setErrorMessage("");
        this.data = data;
        return this;
    }

    public Response success(CommonCode responseKey, String logId) {
        this.meta = new Meta(responseKey.code(), responseKey.msg(), logId);
        return this;
    }

    public Response failure(int errCode, String errMsg) {
        this.meta = new Meta(errCode, errMsg);
        return this;
    }

    public Response failure(CommonCode code, String logId) {
        this.meta = new Meta(code.code(), code.msg(), logId);
        return this;
    }

    public static Response fail(CommonCode code, String logId) {
        Response response = new Response();
        response.failure(code, logId);
        return response;
    }

    public static Response signError(String logId) {
        Response response = new Response();
        //response.meta = new Meta();
        response.failure(CommonCode.FAIL_SIGN_ERROR, logId);
        return response;
    }

    public boolean checkSuccess() {
        if (this.meta == null) {
            return false;
        }
        return this.meta.getErrorCode() == 0;
    }

    public Meta getMeta() {
        return this.meta;
    }

    public Object getData() {
        return this.data;
    }


    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Response{" +
                "meta=" + meta +
                ", data=" + data +
                '}';
    }
}
