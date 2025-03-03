# 中台homo-common-proxy文档
## 设计目标 
**homo-common-proxy**
用来对接所有的公共的中台服务，如邮件、公告、问卷等，主要功能是根据业务的需求转发对应的服务，目的是减少游戏的配置，不需要配置和管理多个服务的地址，在对接时只需要协商服务名和消息id即可。
## 简述

**homo-common-proxy**提供基于homo-core框架的GRPC的消息转发和http请求代理。

**http://inner-common-http-proxy:33306**内网proxy地址

**http://common-http-proxy:33306**现网proxy地址

### 服务关系图

![homo-common-proxy](images/homo-common-proxy.jpg)

### 概念说明

+ **homo-common-server   中台proxy及辅助的服务**
    + 对外暴露的唯一proxy服务器，接受http请求，提供服务代理和转发的功能
+ **http服务器**
    + 对外提供http服务的服务器
+ **grpc的服务器**
    + 中台服务

## 使用说明

### 基于homo-core服务的转发
#### 验证的选择

proxy不做业务上的设置，提供两种验证方式

+ 对发起者的token进行验证
+ 用key加密生成sign，验证sign的正确性

#### 消息头headers
    String X_HOMO_APP_ID = "X-Homo-App-Id";
    String X_HOMO_TOKEN = "X-Homo-Token";
    String X_HOMO_USER_ID = "X-Homo-User-Id";
    String X_HOMO_CHANNEL_ID = "X-Homo-Channel-Id";
    String X_HOMO_SIGNATURE = "X-Homo-Signature";
    String X_HOMO_RESPONSE = "X-Homo-Response";
    String X_HOMO_RESPONSE_TIME = "X-Homo-Response-Time";
+ **非游戏业务**

| 参数名          | 说明           | 示例                                  |
| --------------- | -------------- | ------------------------------------- |
| X-Homo-App-Id    | 游戏id         | heads.put("X-Homo-App-Id", "1000004"); |
| X-Homo-Signature | 经过计算的sign | heads.put("X-Homo-Signature", "sign"); |

+ **游戏业务带token**

| 参数名           | 说明                | 示例                                     |
| ---------------- | ------------------- | ---------------------------------------- |
| X-Homo-App-Id     | 游戏id              | heads.put("X-Homo-App-Id", "1000004");    |
| X-Homo-Token      | 玩家的登录后的token | heads.put("X-Homo-Token", "token");       |
| X-Homo-User-Id    | 与token绑定的userId | heads.put("X-Homo-User-Id", "1000004_2"); |
| X-Homo-Channel-Id | 渠道id              | heads.put("X-Homo-Channel-Id", "2");      |

+ srcService：提供服务的服务名，需要带端口，如configCenterService
+ msgId：消息id，如getTableRecord
+ msgContent：转发的业务需求的数据，具体数据根据业务的文档确定
#### url选择

+ http://homo-common-proxy:33306/clientJsonMsgCheckSign：json格式的转发只检查sign

+ http://homo-common-proxy:33306/clientJsonMsgCheckToken：json格式的转发只检查token

+ http://homo-common-proxy:33306/clientPbMsgCheckSign：pb格式的转发只检查sign

+ http://homo-common-proxy:33306/clientPbMsgCheckToken：pb格式的转发只检查token

#### 返回的格式

```json
{
    "code":"",
    "msg":"",
    "msgContent":"",
    "msgId":""
}
```
+ code：proxy返回的错误码，转发消息的结果，1代表成功，其他表示失败
+ msg：失败原因，code不等于1时msg字段的值有意义

+ msgContent：业务返回的结果
+ msgId：消息id，如getTableRecord

### http请求代理

> http请求代理是通过common-proxy转发任意的http请求，原封不动的转发客户端的请求头和请求体

#### 特殊的请求头

| 参数名              | 说明                                                         | 示例                                       |
| ------------------- | ------------------------------------------------------------ | ------------------------------------------ |
| X-Homo-Response-Time | common-proxy发送请求的超时时间，超时common-proxy将不会等待响应，应小于客户端的超时时间，单位毫秒 | heads.put("X-Homo-Response-Time", "10000"); |
