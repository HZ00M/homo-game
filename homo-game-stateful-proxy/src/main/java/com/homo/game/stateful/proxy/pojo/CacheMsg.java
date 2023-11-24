package com.homo.game.stateful.proxy.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@AllArgsConstructor
@Data
@ToString(exclude = "bytes")
public class CacheMsg {
    public String msgId;
    public byte[] bytes;
    public short sessionId;
    public short innerSendSeq;
    public short clientSendReq;
}
