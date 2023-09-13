package com.homo.game.stateful.proxy.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CacheMsg {
    public String msgId;
    public byte[] bytes;
    public short sessionId;
    public short sendSeq;
    public short recvReq;
}
