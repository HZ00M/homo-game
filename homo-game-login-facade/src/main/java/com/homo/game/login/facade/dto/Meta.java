package com.homo.game.login.facade.dto;

import java.io.Serializable;

public class Meta implements Serializable {
    /**
     * 响应数据中的元数据 表示操作是否成功与返回值消息
     *
     * @author yangsiqin
     * @ClassName: Meta
     * @date 2017年8月25日 上午11:15:38
     */

    // code：msg 一一0:"验证通过"
    private int errorCode;
    private String errorMessage;

    public Meta() {

    }

    public Meta(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    public Meta(int errorCode, String errorMessage, String logId) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        //this.logId = logId;
    }
    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }



    @Override
    public String toString() {
        return "Meta{" +
                "errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
