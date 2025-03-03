package com.homo.entity.test.entity;

import com.core.ability.base.BaseAbilityEntity;
import com.homo.core.utils.rector.Homo;
import com.homo.core.utils.spring.GetBeanUtil;
import com.homo.entity.test.facade.entity.IUerEntity;
import com.homo.game.stateful.proxy.facade.IStatefulProxyService;
import io.homo.proto.client.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserEntity extends BaseAbilityEntity implements IUerEntity {
    public int sendSeq = 1;
    public int testMethodCallCount = 0;

    @Override
    public Homo<TestRsp> test(TestReq testReq) {
        log.info("UserEntity test testReq {}", testReq);
        TestRsp testRsp = TestRsp.newBuilder().setCode(1).setMsg("123").build();
        testMethodCallCount ++;
        return Homo.result(testRsp);
    }

//    @Override
//    public void afterInit() {
//        //entity捞起且缓存起来，所有ability绑定完成完成后，调用
//        log.info("afterInit call id {} ", getOwnerId());
//        try {
//            getAbility(TimeAbility.class).schedule("test", new Runnable() {
//                @Override
//                public void run() {
//                    log.info("UserEntity schedule run id {} type {}",getId(),getType());
//                    scheduleSendMsgTest();
//                }
//            },1000,1000);
//        }catch (Exception e){
//            log.error("UserEntity schedule run error id {} type {} e",getId(),getType(),e);
//        }
//
//    }

    public void scheduleSendMsgTest() {
        IStatefulProxyService proxyService = GetBeanUtil.getBean(IStatefulProxyService.class);
        InnerSendMsg msg = InnerSendMsg.newBuilder().setMsg("scheduleSendMsg").setCode(111222333).build();
        ToClientReq toClientReq = ToClientReq.newBuilder()
                .setClientId(getOwnerId())
                .setMsgContent(msg.getMsgBytes())
                .setMsgType(InnerSendMsg.class.getSimpleName())
                .build();
        ParameterMsg parameterMsg = ParameterMsg.newBuilder().setUserId(getOwnerId()).build();
        proxyService.sendToClient(-1, parameterMsg, toClientReq).start();
        sendSeq++;
        log.info("UserEntity scheduleSendMsgTest id {} type {} sendSeq {}",getId(),getType(),sendSeq);
    }
}
