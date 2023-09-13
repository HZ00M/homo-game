package com.homo.game.stateful.proxy.facade;

import com.homo.core.facade.ability.EntityType;
import com.homo.core.utils.rector.Homo;

@EntityType(type = "client",isLocalService = true)
public interface IClientSendHandler {
    Homo<Void> send(String msgId,byte[] msg);

    Homo<Void> sendWithSessionId(String msgId,byte[] msg,short sessionId);

    void sendToAll(String msgId,byte[] msg,short sessionId,short sendSeq,short recReq);

    Homo<Boolean> sendComplete(String msgId,byte[] msg);
}
