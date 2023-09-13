package com.homo.entity.test.entity;

import com.core.ability.base.AbstractAbilityEntity;
import com.homo.core.facade.ability.SaveAble;
import com.homo.core.utils.rector.Homo;
import com.homo.entity.test.facade.entity.IUerEntity;
import io.homo.proto.client.TestReq;
import io.homo.proto.client.TestRsp;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserEntity extends AbstractAbilityEntity implements SaveAble ,IUerEntity {
    @Override
    public Homo<TestRsp> test(TestReq testReq) {
        log.info("UserEntity test testReq {}",testReq);
        TestRsp testRsp = TestRsp.newBuilder().setCode(1).setMsg("123").build();
        return Homo.result(testRsp);
    }
}
