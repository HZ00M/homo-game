package com.homo.game.proxy.proxy.facade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientJsonRouterMsg implements Serializable {
    String serviceName;
    Integer podIndex;
    String msgId;
    String msgContent;
}
