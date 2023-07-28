package com.homo.game.login.facade.dto;

public enum CommonCode {
    SUCCESS("操作成功", 0),
    FAIL_GAME("分区错误", 1001),
    FAIL_SYSTEM("系统异常", 1002),
    FAIL_SIGN_ERROR("签名认证失败", 1003),
    FAIL_NOT_NULL("必须参数为空", 1004),
    FAIL_EMPTY_ACCOUNT("帐户不存在", 1005),
    FAIL_PARAM_ERROR("参数错误", 1006),
    FAIL_SYSTEM_BUSY("系统繁忙", 1007),
    FAIL_MESSAGE_LIMIT_EXCEEDED("短信次数超出限制", 1008),
    FAIL_MESSAGE_SEND("短信发送失败", 1009),
    FAIL_ACCOUNT_ILLEGAL("帐号不符合要求", 1010),
    FAIL_VERIFICATION_CODE_ERROR("验证码校验失败", 1011),
    FAIL_TLENL_ERROR("tokenL已失效", 1012),
    FAIL_ERROR_PASSWORD("密码错误", 1013),
    FAIL_EXIST_PHONE_BINDING_ERROR("此手机号已被绑定", 1014),
    FAIL_ERROR_CODE("短信验证码校验失败", 1015),
    FAIL_ERROR_ACCOUNT_PHONE("帐号已经绑定手机号", 1016),
    FAIL_EMAIL_BINDED("帐号已经绑定邮箱", 1017),
    FAIL_EMAIL_CODE("邮箱验证码校验错误", 1018),
    FAIL_ACCOUNT_BINDED("帐号已绑定", 1019),
    FAIL_EXIST_EMAIL("邮箱已存在", 1020),
    FAIL_RELATED_PHONE_EXIST("已经关联过手机号", 1021),
    FAIL_RELATED_PHONE_ERR("关联的手机号错误", 1024),
    FAIL_EMPTY_USER_ERROR("实名不正确", 1026),
    FAIL_ID_CARD_ILLEGAL("身份证不合法", 1025),
    FAIL_GAME_CLOSE("游戏未开放", 1030),
    FAIL_IP_OUT_RANGE("IP不在范围内", 1031),
    FAIL_DISABLE_TOURIST("游客注册已禁用", 1032),

    //激活码
    FAIL_ACTIVATION_CODE_NONE("帐号未激活", 1033),
    FAIL_ACTIVATION_CODE_USED("激活码已被使用", 1034),
    FAIL_ACTIVATE_ERROR("激活码接口内部错误", 1035),

    FAIL_ACCOUNT_REGISTER("帐户不允许注册", 1036),
    FAIL_EXIST_ACCOUNT("帐户已存在", 1037),
    FAIL_BINDING_DEVICE_ILLEGAL("设备标识不匹配", 1038),
    FAIL_ACCOUNT_INVALID("帐户不允许登陆", 1039),
    FAIL_ERROR_TOKEN("Token错误", 1040),
    FAIL_EXIST_USER_IDCARD("已经实名认证过", 1041),
    FAIL_FREZZ_IDENTITY("认证次数超出限制", 1042),
    FAIL_IN_PROGRESS("认证中",1045),
    FAIL_FREZZ_IDENTITY_MANUAL("认证次数超出限制,可通过人工审核方式进行认证",1046),

    FAIL_EMPTY_ACCOUNTNAME("用户名或手机号码为空", 1043),
    FAIL_EMPTY_PASSWORD("密码或者验证码为空", 1044),
    //白名单
    FAIL_OUT_WHITECOUNT("白名单数量超过限制(在白名单内）", 1050),
    FAIL_ACCOUNT_OUT_WHITELIST_FULL("超出白名单数量限制（不在白名单内）", 1051),
    FAIL_ACCOUNT_OUT_WHITELIST("不允许的帐户（不在白名单内）", 1052),
    FAIL_OUT_LOGINCOUNT("登陆数量超过限制", 1053),


    FAIL_FREZZ_MSGCODE("发送验证码冷却中，请稍后再来", 1060),
    FAIL_EMPTY_MSGCODE("没有数据", 1061),
    FAIL_INVALID_CONFIG_MSGCODE("无效配置项", 1062),
    FAIL_PARAM_ERROR_MSGCODE("参数错误", 1064),
    FAIL_FREZZ_MSGCODE_INTERVAL("短时间内认证次数超过限制", 1065),

    FAIL_NULL_PASSWORD("未设置密码", 1070),

    FAIL_GAME_EXIST("分区已存在", 1100),

    //激活码
    FAIL_ACTIVAE_EMPTY("卡号为空", 1200),
    FAIL_ACTIVAE_NONE("卡不存在", 1201),
    FAIL_ACTIVAE_MULTI("卡不唯一", 1202),
    FAIL_ACTIVAE_STAY("卡未被领取", 1203),
    FAIL_ACTIVAE_NOTFIT("卡已经绑定帐号，且帐号不匹配", 1204),
    FAIL_ACTIVAE_EXPIRED("卡已过期", 1205),
    FAIL_ACTIVAE_UNKNOWNACCOUNT("帐号未知", 1206),
    FAIL_ACTIVAE_ERRORPLAYER("该卡已经绑定角色id，且角色id不匹配", 1207),
    FAIL_ACTIVAE_ERRORSERVER("该卡已经绑定服务器，且服务器id不匹配", 1208),
    FAIL_ACTIVAE_REPEATED("该类卡属于互斥卡，且互斥的卡已经使用过", 1209),
    FAIL_ACTIVAE_TOOMUCH("该类卡已超过使用次数限制", 1210),
    FAIL_ACTIVAE_CARDTYPEERRSERVER("该类卡已经绑定服务器，且服务器id不匹配", 1211),
    FAIL_ACTIVAE_CARDPLATERROR("该卡已经绑定平台，且平台id不匹配", 1212),
    FAIL_ACTIVAE_CARDTYPEPLATERROR("该卡已经绑定平台，且平台id不匹配", 1213),
    FAIL_ACTIVAE_PLAYERIDERROR("player id未知", 1214),
    FAIL_ACTIVAE_USED("卡已经被其他人使用", 1215),

    FAIL_PERMISSION_DENINY("权限不足", 1300),
    FAIL_RIGHT_NULL("用户权限不存在", 1301),

    FAIL_NO_USER("用户不存在", 1350),
    FAIL_PIC_CODEERR("图片验证码错误", 1400),

    FAIL_FREZZ_PASS("错误次数太多，请稍后再试", 1450),

    CHANNEL_TOKEN_ERROR("渠道认证失败", 1452),
    CHANNEL_NOT_EXIST("渠道不存在", 1453),

    FAIL_LOGIN_OUT("登出失败",1451),

    LOGIN_NEED_BINDING_PHONE("需要绑定手机号", 1459);


    private String msg ;
    private int code ;

    private CommonCode(String msg , int code ){
        this.msg = msg ;
        this.code = code ;
    }

    public int code(){
        return this.code;
    }

    public String msg(){
        return this.msg;
    }
}
