package com.homo.entity.test.facade.entity;

import com.homo.core.facade.ability.CacheTime;
import com.homo.core.facade.ability.EntityType;
import com.homo.core.facade.ability.StorageTime;
import com.homo.core.utils.rector.Homo;
import io.homo.proto.client.TestReq;
import io.homo.proto.client.TestRsp;

@EntityType(type = "user")
@StorageTime(10000)
@CacheTime(10000)
public interface IUerEntity {
    Homo<TestRsp> test(TestReq testReq);
}
