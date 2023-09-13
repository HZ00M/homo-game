//package com.homo.game.stateful.proxy.handler;
//
//import com.core.ability.base.AbstractAbilityEntity;
//import com.homo.core.utils.rector.Homo;
//import com.homo.game.stateful.proxy.facade.IClientSendHandler;
//import com.homo.game.stateful.proxy.gate.ProxyGateClient;
//import lombok.Getter;
//import lombok.Setter;
//import lombok.extern.slf4j.Slf4j;
//
///**
// * 为了支持后端服务器与客户端通讯，通过改entity与gateClient通讯
// * 这个entity不落地，只提供rpc调用支持
// */
//@Slf4j
//public class ClientSendHandler extends AbstractAbilityEntity implements IClientSendHandler {
//    @Setter
//    @Getter
//    ProxyGateClient gateClient;
//
//    @Override
//    protected Homo<Void> afterPromiseInit() {
//        return Homo.result(null);
//    }
//
//    @Override
//    public Homo<Void> send(String msgId, byte[] msg) {
//        return null;
//    }
//
//    @Override
//    public Homo<Void> sendWithSessionId(String msgId, byte[] msg, short sessionId) {
//        gateClient.recvTransferMsgs()
//        return null;
//    }
//
//    @Override
//    public void sendToAll(String msgId, byte[] msg, short sessionId, short sendSeq, short recReq) {
//
//    }
//
//    @Override
//    public Homo<Boolean> sendComplete(String msgId, byte[] msg) {
//        return null;
//    }
//}
