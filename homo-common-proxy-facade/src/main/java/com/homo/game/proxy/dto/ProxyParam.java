package com.homo.game.proxy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class ProxyParam implements Serializable {
    /**
     * 转发服务器名
     */
    private String srcService;

    /**
     * 消息Id
     */
    private String msgId;

    /**
     * 消息内容字符串
     */
    private String msgContent;
}
