package com.homo.game.proxy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 专门用来处理http转发的消息头和返回的结果
 */
@Data
@Builder
@AllArgsConstructor
public class HttpParam implements Serializable {
    private int code;

    private String headers;

    private String result;
}
