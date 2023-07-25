package com.homo.game.proxy.proxy.facade;

import lombok.Data;

import java.io.Serializable;

@Data
public class ClientJsonRouterMsg implements Serializable {
    String serviceName;
    Integer podIndex;
    String msgId;
    String msgContent;
}
