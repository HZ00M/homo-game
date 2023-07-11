package com.homo.common.proxy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ResponseMsg {
    /**
     * 转发消息的结果，200代表成功，其他表示失败
     */
    private Integer code;
    /**
     * 失败原因，code不等于1时msg字段的值有意义
     */
    private String msg;
    /**
     * 消息id
     */
    private String msgId;
    /**
     * 业务返回的结构
     */
    private String msgContent;
}
