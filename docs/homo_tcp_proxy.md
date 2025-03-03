# 中台homo-tcp-proxy文档
## 设计目标 
**homo-tcp-proxy**
用于TCP长连接服务的统一入口，外部消息通过该proxy将tcp消息转发到对应的服务，服务的消息也可以通过proxy发送到客户端。
## 简述

**homo-tcp-proxy**提供基于homo-core框架的TCP的消息转发。

**http://homo-tcp-proxy:666**proxy地址
  
### 概念说明 
+ **homo-tcp-proxy**
    + 对外提供TCP消息转发服务
  
## 使用说明 
 
#### url选择 
+ tcp://homo-tcp-proxy:666 用于处理proto协议的转发 
## 请求协议
```protobuf
//登陆连接验证
message LoginMsgReq {
  string token          = 1; //从登陆平台获得的token
  string userId         = 2; //用户id
  string channelId      = 3; //用户渠道
  string appVersion = 4;  //客户端版本
  string resVersion = 5;  //资源版本
  string adId = 6;
}
//连接验证成功后就可以发送tcp消息
message LoginMsgResp {
  int32 errorCode = 1;
  string errorMsg     = 2;
}
//断线重连
message LoginAndSyncReq{
  LoginMsgReq loginMsgReq = 1; //登陆参数
  SyncInfo syncInfo = 2;  // 断线重连发送的同步信息，服务器会根据客户端当前接受的包序列号，尝试将服务器发送的未被接收的数据进行补发
}

message LoginAndSyncResp{
  LoginMsgResp loginMsgResp = 1;
}

//断线重连成功时如果存在未发送数据，服务器内部会主动调用TransferCacheReq获取续传数据
message TransferCacheReq {
  string userId = 1;
  int32 podId = 2;
  SyncInfo syncInfo = 3;
}
//断线重连成功后，服务器会通过tcp连接发送该续传消息
message TransferCacheResp {
  enum ErrorType {
    OK = 0;
    ERROR = 1;
    USER_NOT_FOUND = 2;
    CACHE_INVALID = 3;
  }
  message SyncMsg{
    string msgId = 1;
    //客户端proxy传输协议
    bytes msg = 2;
    //消息sessionId
    int32 sessionId = 3;
    //消息的发送序号
    int32 sendSeq = 4;
    //消息的确认号
    int32 recReq = 5;
  }
  ErrorType errorCode = 1;
  string errorMsg = 2;
  int32 sendSeq = 3;  //当前服务器发送序号
  int32 recvSeq = 4;  //当前服务器接收到序号
  repeated SyncMsg syncMsgList = 5; //续传消息
}

// 有状态proxy断开连接消息
message DisconnectMsg{
  enum ErrorType //枚举消息类型
  {
    OTHER_USER_LOGIN = 0;//顶号
    OTHER_USER_RECONNECT = 1;//重连
    SERVER_CLOSE = 2;  // 关服
  }
  ErrorType errorCode = 1;
  string errorMsg = 2;
}

// 踢人消息
message KickUserMsg{
  string kickReason = 1;    // 被踢原因
}

message StateOfflineRequest{
  string reason = 1; //关闭原因
  int64  time = 2;  //关闭时间
}

```
 