package com.homo.game.test.service;

import com.homo.core.root.storage.RectorEntityStorage;
import com.homo.core.utils.rector.Homo;
import com.homo.game.test.entity.User;
import com.homo.game.test.facade.AppUserService;
import com.homo.game.test.facade.LogicType;
import io.homo.game.client.msg.proto.CreateUserReq;
import io.homo.game.client.msg.proto.CreateUserResp;
import io.homo.game.client.msg.proto.GetUserInfoReq;
import io.homo.game.client.msg.proto.GetUserInfoResp;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class AppUserServiceImpl implements AppUserService {
    @Autowired
    RectorEntityStorage<Bson, Bson, Bson, List<Bson>> entityStorage;

    public Homo<GetUserInfoResp> getUserInfo(GetUserInfoReq req) {
        log.info("getUserInfo req userId_{} req_{}", req.getUserId(), req);
        return entityStorage
                .get(LogicType.USER.name(), req.getUserId(), "data", User.class)
                .nextDo(retUser -> {
                    GetUserInfoResp res;
                    if (retUser != null) {
                        res = GetUserInfoResp.newBuilder()
                                .setErrorCode(0)
                                .setErrorDesc("获取成功")
                                .setUserInfo(User.covertUserInfoToProto(retUser))
                                .build();
                    } else {
                        res = GetUserInfoResp.newBuilder()
                                .setErrorCode(1)
                                .setErrorDesc("没有该用户信息")
                                .build();
                    }
                    log.info("getUserInfo res userId_{} res_{}", req.getUserId(), res);
                    return Homo.result(res);
                }).onErrorContinue(throwable -> {
                    GetUserInfoResp res = GetUserInfoResp.newBuilder()
                            .setErrorCode(2)
                            .setErrorDesc("服务器异常")
                            .build();
                    log.error("getUserInfo error userId_{} req_{} res_{}", req.getUserId(), req, res);
                    return Homo.result(res);
                });
    }

    @Override
    public Homo<CreateUserResp> createInfo(CreateUserReq req) {
        log.info("createUserInfo req userId_{} req_{}", req.getUserInfo().getUserId(), req);
        String userId = req.getUserInfo().getUserId();
        String uuid = UUID.randomUUID().toString();
        return entityStorage
                .asyncLock(LogicType.USER.name(), userId, "user", "", uuid, 5, 5, 1)
                .nextDo(aBoolean -> {
                    if (!aBoolean) {
                        log.error("createUserInfo lock fail userId_{} ", userId);
                        return Homo.error(new Error());
                    }
                    log.info("createUserInfo lock success userId_{} ", userId);
                    return Homo.result(1);
                })
                .nextDo(ret ->
                        entityStorage.get(LogicType.USER.name(), userId, "data", User.class)
                                .nextDo(newUser -> {
                                    if (newUser == null) {
                                        boolean isPass = User.checkCreateInfo(req.getUserInfo());
                                        if (!isPass) {
                                            return Homo.error(new Exception("参数错误"));
                                        }
                                        newUser = User.createUser(req.getUserInfo());
                                        return entityStorage.save(LogicType.USER.name(), userId, "data", newUser, User.class);
                                    }
                                    return Homo.error(new Exception("用户已存在"));
                                })
                                .nextDo(saveUser -> Homo.result(CreateUserResp.newBuilder().setErrorCode(0).setErrorDesc("创建成功").setUserInfo(User.covertUserInfoToProto(saveUser)).build())
                                        .onErrorContinue(throwable -> {
                                            CreateUserResp res = CreateUserResp.newBuilder()
                                                    .setErrorCode(1)
                                                    .setErrorDesc(throwable.getMessage())
                                                    .build();
                                            log.info("createUserInfo fail userId_{} req_{} res_{}", userId, req, res);
                                            return Homo.result(res);
                                        })
                                )
                                .nextDo(resp ->
                                        entityStorage
                                                .asyncUnlock(LogicType.USER.name(), userId, "user", uuid)
                                                .nextDo(unlock -> Homo.result(resp))
                                )
                );
    }

}
