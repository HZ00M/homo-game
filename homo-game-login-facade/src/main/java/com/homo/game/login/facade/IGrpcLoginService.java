package com.homo.game.login.facade;

import com.homo.core.facade.rpc.RpcHandler;
import com.homo.core.facade.rpc.RpcType;
import com.homo.core.facade.service.ServiceExport;
import com.homo.core.utils.rector.Homo;
import com.homo.game.login.facade.dto.Response;
import com.homo.game.login.proto.Auth;
import io.homo.proto.client.ParameterMsg;

@ServiceExport(tagName = "login-service-grpc:31504",driverType = RpcType.grpc,isStateful = false,isMainServer = false)
@RpcHandler
public interface IGrpcLoginService {

    /**
     * 给其他游戏服务器调用的鉴权接口
     * @param parameterMsg userId
     * @param auth 鉴权消息
     */
    Homo<Response> auth(ParameterMsg parameterMsg, Auth auth);

    /**
     * 查询用户信息
     * @param auth
     * @return
     */
    Homo<Response> queryUserInfo(Auth auth);

    /**
     * 发送短信验证码
     * @param param 用户认证
     * @param fetchType 验证码类型
     * @return 操作结果
     */
    Homo<Response> sendPhoneCode(Auth param, String phone, String fetchType);
    /**
     * 验证短信验证码的准确性
     * @param phone 手机号
     * @param fetchType 认证类型
     * @return 操作结果
     */
    Homo<Response> validatePhoneCode(Auth param, String phone, String code, String fetchType);

}
