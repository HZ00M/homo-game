package com.homo.game.test.facade;


import com.homo.core.utils.rector.Homo;
import io.homo.game.client.msg.proto.CreateUserReq;
import io.homo.game.client.msg.proto.CreateUserResp;
import io.homo.game.client.msg.proto.GetUserInfoReq;
import io.homo.game.client.msg.proto.GetUserInfoResp;
import org.junit.jupiter.params.shadow.com.univocity.parsers.common.input.CharInputReader;

/**
 * syclub-app grpc服务
 * @author dubian
 */
public interface AppUserService  {

    Homo<GetUserInfoResp> getUserInfo(GetUserInfoReq req);

    Homo<CreateUserResp> createInfo(CreateUserReq req);
}
