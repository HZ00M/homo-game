package com.homo.game.test.service.test;

import com.alibaba.fastjson.JSONObject;
import com.homo.core.facade.cache.CacheDriver;
import com.homo.core.root.storage.EntityStorage;
import com.homo.core.utils.callback.CallBack;
import com.homo.core.utils.lang.Pair;
import com.homo.game.test.TestApplication;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@Slf4j
public class RedisTest {
    @Autowired
    CacheDriver cacheDriver;
    @Autowired
    EntityStorage storage;
    @Data
    @AllArgsConstructor
    public class CacheBody {
        String testData;
    }
    @Test
    public void updateTest() throws InterruptedException {
        CacheBody body = new CacheBody("updateTest");
        String msg = JSONObject.toJSONString(body);
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        HashMap<String, byte[]> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("data", msgBytes);
        cacheDriver.asyncUpdate(storage.getServerInfo().getAppId(), storage.getServerInfo().getRegionId(), "logicType", "1", objectObjectHashMap, new CallBack<Boolean>() {
            @Override
            public void onBack(Boolean value) {
                log.info("update result {}", value.toString());
            }

            @Override
            public void onError(Throwable throwable) {
                log.info("update throwable", throwable);
            }
        });
        Thread.currentThread().join();
    }

    @Test
    public void updateExpireTest() throws InterruptedException {
        CacheBody body = new CacheBody("updateExpireTest");
        String msg = JSONObject.toJSONString(body);
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        HashMap<String, byte[]> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("data", msgBytes);
        cacheDriver.asyncUpdate(storage.getServerInfo().getAppId(), storage.getServerInfo().getRegionId(), "logicType", "1", objectObjectHashMap,30, new CallBack<Boolean>() {
            @Override
            public void onBack(Boolean value) {
                log.info("updateExpireTest result {}", value.toString());
            }

            @Override
            public void onError(Throwable throwable) {
                log.info("updateExpireTest throwable", throwable);
            }
        });
        Thread.currentThread().join();
    }

    @Test
    public void getTest() throws InterruptedException {
        cacheDriver.asyncGetAll(storage.getServerInfo().getAppId(), storage.getServerInfo().getRegionId(), "logicType", "1", new CallBack<Map<String, byte[]>>() {
            @Override
            public void onBack(Map<String, byte[]> value) {
                for (Map.Entry<String, byte[]> stringEntry : value.entrySet()) {
                    log.info("asyncGetAll result key {} value {}", stringEntry.getKey(), new String(value.get(stringEntry.getKey())));
                }

            }

            @Override
            public void onError(Throwable throwable) {
                log.info("asyncGetAll throwable", throwable);
            }
        });
        Thread.currentThread().join();
    }

    @Test
    public void removeTest() throws InterruptedException {
        ArrayList arrayList = new ArrayList();
        arrayList.add("data");
        cacheDriver.asyncRemoveKeys(storage.getServerInfo().getAppId(), storage.getServerInfo().getRegionId(), "logicType", "1", arrayList, new CallBack<Boolean>() {
            @Override
            public void onBack(Boolean value) {
                log.info("asyncRemoveKeys result {}", value);
            }

            @Override
            public void onError(Throwable throwable) {
                log.info("asyncRemoveKeys throwable", throwable);
            }
        });
        Thread.currentThread().join();
    }

    @Test
    public void incrTest() throws InterruptedException {
        HashMap<String, Long> map = new HashMap<>();
        map.put("data",1L);
        map.put("test",2L);
        cacheDriver.asyncIncr(storage.getServerInfo().getAppId(), storage.getServerInfo().getRegionId(), "logicType", "1", map, new CallBack<Pair<Boolean, Map<String, Long>>>() {
            @Override
            public void onBack(Pair<Boolean, Map<String, Long>> value) {
                for (Map.Entry<String, Long> entry : value.getValue().entrySet()) {
                    log.info("key {} value{}",entry.getKey(),entry.getValue());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.info("asyncIncr throwable", throwable);
            }
        });
        Thread.currentThread().join();
    }

}
