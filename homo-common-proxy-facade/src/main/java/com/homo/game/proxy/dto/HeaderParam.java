package com.homo.game.proxy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class HeaderParam implements Serializable {
    /**
     * 用户token
     */
    private String token;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 渠道id
     */
    private String channelId;

    /**
     * 游戏id
     */
    private String appId;

    /**
     * sign 验签
     */
    private String sign;

    /**
     * 客户端原字符串，验签用
     */
    private String body;
}
