package com.homo.game.test.entity;


import com.homo.core.facade.document.Document;
import io.homo.game.client.msg.proto.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@BsonDiscriminator
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collectionName = "user", indexes = {"value.nickName", "value.bindGameList.gameName", "value.level", "value.registerTime", "value.tagList"})
public class User implements Serializable {
    private String userId;
    private String phone;
    private String nickName;
    private Integer age;
    private Integer sex;                                                //性别  0男 1女
    private String sign;
    private String birthday;
    private String avatar;
    private String identityId;                                          //身份证
    private String identityIdName;                                      //身份证姓名
    private Integer level;                                              //会员等级
    private Double totalCharge;                                         //总充值金额
    private Integer groupValue;                                         //成长值
    private String imei;                                                //imei
    private String platform;                                            //系统类型 ios or android
    private String deviceType;                                          //手机型号
    private String token;                                               //登陆token

    private Long registerTime;                                          //注册时间
    private Long inMemberTime;                                          //成为会员时间
    private Long lastLoginTime;                                         //最后一次登陆时间
    private Long lastChargeTime;                                        //最后一次充值时间

    private Long deadlineDay; // 成长值最后期限

    private Map<String, BindGame> bindGameMap;                          //已绑定角色列表
    private List<Address> addressList;                                  //地址列表
    private List<String> tagList = new ArrayList() {{                    //用户标签
        add("");
        add("");
        add("");
        add("");
        add("");
        add("");
        add("");
        add("");
    }};

    public static User getUpdatePartial(UserInfo userInfo) {
        User user = new User();
        if (!StringUtils.isEmpty(userInfo.getNickName())) {
            user.nickName = userInfo.getNickName();
        }
        if (userInfo.getAge() > 0) {
            user.age = userInfo.getAge();
        }
        user.sex = userInfo.getSex();
        if (!StringUtils.isEmpty(userInfo.getSign())) {
            user.sign = userInfo.getSign();
        }
        if (!StringUtils.isEmpty(userInfo.getBirthday())) {
            user.birthday = userInfo.getBirthday();
        }
        if (!StringUtils.isEmpty(userInfo.getAvatar())) {
            user.avatar = userInfo.getAvatar();
        }
        if (!StringUtils.isEmpty(userInfo.getIdentityId())) {
            user.identityId = userInfo.getIdentityId();
        }
        if (!StringUtils.isEmpty(userInfo.getIdentityName())) {
            user.identityIdName = userInfo.getIdentityName();
        }
        if (!userInfo.getAddressesList().isEmpty()) {
            user.addressList = userInfo.getAddressesList().stream().map(User::covertAddress).collect(Collectors.toList());
        }

        return user;
    }

    public static User createUser(UserInfo userInfo) {
        User user = new User();
        user.userId = userInfo.getUserId();
        if (!StringUtils.isEmpty(userInfo.getNickName())) {
            user.nickName = userInfo.getNickName();
        } else {
            user.nickName = "未命名";
        }
        user.age = userInfo.getAge();
        user.sex = userInfo.getSex();
        if (!StringUtils.isEmpty(userInfo.getPhone())) {
            user.phone = userInfo.getPhone();
        } else {
            user.sign = "暂无签名";
        }
        if (!StringUtils.isEmpty(userInfo.getSign())) {
            user.sign = userInfo.getSign();
        } else {
            user.sign = "暂无签名";
        }
        if (!StringUtils.isEmpty(userInfo.getBirthday())) {
            user.birthday = userInfo.getBirthday();
        }
        if (!StringUtils.isEmpty(userInfo.getAvatar())) {
            user.avatar = userInfo.getAvatar();
        } else {
            user.avatar = "http://mqfgx.cn/Gpp14";
        }
        if (!StringUtils.isEmpty(userInfo.getIdentityId())) {
            user.identityId = userInfo.getIdentityId();
        }
        if (!StringUtils.isEmpty(userInfo.getIdentityName())) {
            user.identityIdName = userInfo.getIdentityName();
        }
        if (!StringUtils.isEmpty(userInfo.getImei())) {
            user.imei = userInfo.getImei();
        }
        if (!StringUtils.isEmpty(userInfo.getPlatform())) {
            user.platform = userInfo.getPlatform();
        }
        if (!StringUtils.isEmpty(userInfo.getDeviceType())) {
            user.deviceType = userInfo.getDeviceType();
        }
        if (!StringUtils.isEmpty(userInfo.getToken())) {
            user.token = userInfo.getToken();
        }
        user.level = 0;
        user.totalCharge = 0.0d;
        user.groupValue = 10; // 用户第一次注册 成长值+10
        user.registerTime = System.currentTimeMillis();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 365);
        user.deadlineDay = calendar.getTime().getTime(); // 成长值有效期

        user.inMemberTime = null;
        user.lastLoginTime = null;
        user.lastChargeTime = null;
        if (!userInfo.getAddressesList().isEmpty()) {
            user.addressList = userInfo.getAddressesList().stream().map(User::covertAddress).collect(Collectors.toList());
        }

        if (!userInfo.getHadBindGamesList().isEmpty()) {
            user.bindGameMap = userInfo.getBindGamesList().stream().map(User::covertBindGame).collect(Collectors.toMap(bindGame -> bindGame.appId, Function.identity()));
        }
        return user;
    }

    public static UserInfo covertUserInfoToProto(User user) {
        return UserInfo.newBuilder()
                .setUserId(user.getUserId())
                .setPhone(Optional.ofNullable(user.getPhone()).orElse(""))
                .setSign(Optional.ofNullable(user.getSign()).orElse(""))
                .setAvatar(Optional.ofNullable(user.getAvatar()).orElse(""))
                .setAge(Optional.ofNullable(user.getAge()).orElse(0))
                .setSex(Optional.ofNullable(user.getSex()).orElse(0))
                .setGroupValue(Optional.ofNullable(user.getGroupValue()).orElse(0))
                .setBirthday(Optional.ofNullable(user.getBirthday()).orElse("1970-01-01"))
                .setLevel(Optional.ofNullable(user.getLevel()).orElse(0))
                .setIdentityId(Optional.ofNullable(user.getIdentityId()).orElse(""))
                .setIdentityName(Optional.ofNullable(user.getIdentityIdName()).orElse(""))
                .setImei(Optional.ofNullable(user.getImei()).orElse(""))
                .setDeviceType(Optional.ofNullable(user.getDeviceType()).orElse(""))
                .setToken(Optional.ofNullable(user.getToken()).orElse(""))
                .setPlatform(Optional.ofNullable(user.getPlatform()).orElse(""))
                .setNickName(Optional.ofNullable(user.getNickName()).orElse(""))
                .setLastLoginTime(Optional.ofNullable(user.getLastLoginTime()).orElse(0L))
                .setLastChargeTime(Optional.ofNullable(user.getLastChargeTime()).orElse(0L))
                .addAllAddresses(Optional.ofNullable(user.getAddressList()).orElse(new ArrayList<Address>()).stream().map(User::covertAddressToProto).collect(Collectors.toList()))
                .addAllHadBindGames(Optional.ofNullable(user.getBindGameMap()).orElse(new HashMap<>()).values().stream().map(User::covertBindGameToProto).collect(Collectors.toList()))
                .build();
    }

    public static io.homo.game.client.msg.proto.Address covertAddressToProto(Address address) {
        return io.homo.game.client.msg.proto.Address.newBuilder()
                .setProvince(address.province)
                .setCity(address.city)
                .setCounty(address.county)
                .setMark(address.mark)
                .build();
    }


    public static io.homo.game.client.msg.proto.BindGame covertBindGameToProto(BindGame bindGame) {
        return io.homo.game.client.msg.proto.BindGame.newBuilder()
                .setGameId(Optional.ofNullable(bindGame.getGameId()).orElse(""))
                .setAppId(Optional.ofNullable(bindGame.getAppId()).orElse(""))
                .setChannelId(Optional.ofNullable(bindGame.getChannelId()).orElse(""))
                .setAppName(Optional.ofNullable(bindGame.getAppName()).orElse(""))
                .setRoleId(Optional.ofNullable(bindGame.getRoleId()).orElse(""))
                .setRole(Optional.ofNullable(bindGame.getRole()).orElse(""))
                .build();
    }

    public static Address covertAddress(io.homo.game.client.msg.proto.Address address) {
        return new Address(address.getProvince(), address.getCity(), address.getCounty(), address.getMark());
    }

    public static BindGame covertBindGame(io.homo.game.client.msg.proto.BindGame bindGame) {
        return new BindGame(bindGame.getAppId(), bindGame.getAppName(), bindGame.getChannelId(), bindGame.getGameId(), bindGame.getRole(), bindGame.getRoleId());
    }

    public static boolean checkCreateInfo(UserInfo userInfo) {
        if (userInfo.getAge() < 0 || userInfo.getAge() > 100) {
            return false;
        }
        if (userInfo.getSex() < 0 || userInfo.getSex() > 2) {
            return false;
        }
        return true;
    }

    public static boolean checkUpdateInfo(UserInfo userInfo) {
        if (userInfo.getAge() < 0 || userInfo.getAge() > 100) {
            return false;
        }
        if (userInfo.getSex() < 0 || userInfo.getSex() > 2) {
            return false;
        }
        return true;
    }

}
