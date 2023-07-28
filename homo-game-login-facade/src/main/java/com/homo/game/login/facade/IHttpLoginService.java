package com.homo.game.login.facade;

import com.alibaba.fastjson.JSONObject;
import com.homo.core.facade.rpc.RpcHandler;
import com.homo.core.facade.rpc.RpcType;
import com.homo.core.facade.service.ServiceExport;
import com.homo.core.utils.rector.Homo;
import com.homo.game.login.facade.dto.Response;
import com.homo.game.login.proto.Auth;
import io.homo.proto.client.ParameterMsg;

@ServiceExport(tagName = "login-service-http:31505",driverType = RpcType.http,isStateful = false,isMainServer = true)
@RpcHandler
public interface IHttpLoginService {
    /**
     * 用户注册
     * @param param 参数
     * @param header 请求头
     */
    Homo<String> register(JSONObject param, JSONObject header);
    /**
     * 给其他游戏服务器调用的鉴权接口
     * @param param 参数
     * @param header 请求头
     */
    Homo<String> login(JSONObject param, JSONObject header);

}
