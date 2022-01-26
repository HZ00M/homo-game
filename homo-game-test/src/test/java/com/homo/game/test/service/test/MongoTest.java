package com.homo.game.test.service.test;


import com.homo.game.test.TestApplication;
import com.homo.game.test.facade.AppUserService;
import io.homo.game.client.msg.proto.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@Slf4j
public class MongoTest {
    @Autowired
    private AppUserService appUserService;
    String userId = "1";

    @Test
    public void create() throws InterruptedException {

        Address address = Address.newBuilder().setCounty("中国").setProvince("广东").setCity("深圳").setMark("南山").build();
        List<Address> addressList = new ArrayList<>();
        addressList.add(address);
        BindGame bindGame = BindGame.newBuilder().setAppId("100004").setAppName("无悔入华夏").setGameId("万千少女的梦").build();
        List<BindGame> bindGameList = new ArrayList<>();
        bindGameList.add(bindGame);
        appUserService.createInfo( CreateUserReq.newBuilder()
                .setUserInfo(
                        UserInfo.newBuilder()
                                .setUserId(userId)
                                .setNickName("周杰伦")
                                .setSign("签名")
                                .setLevel(1)
                                .setAge(24)
                                .setAvatar("头像")
                                .setPhone("13160677962")
                                .addAllAddresses(addressList)
                                .addAllBindGames(bindGameList)
                                .build())
                .build()).start();
        Thread.currentThread().join();
    }


    @Test
    public void testGetUserInfo()throws InterruptedException{
        appUserService.getUserInfo(GetUserInfoReq.newBuilder().setUserId(userId).build()).start();
        Thread.currentThread().join();
    }
}
