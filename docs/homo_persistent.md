# 中台homo-game-persistent-server文档
## 设计目标 
**homo-game-persistent-server**
主要用于针对使用了Homo-core框架的服务产生的用户数据进行自动存储落地功能，用户只需要关注本身的业务逻辑，过程中改变的用户数据由框架自动进行存储。
## 简述

**homo-game-persistent-server**提供基于redis和mysql实现自动落地能力，玩家的所有修改都只是对二级缓存的修改（本地内存），程序会自动将本地内存
同步到redis与mysql，无需程序员自己编写存储更新逻辑

 

## 使用说明 
```text
直接运行落地程序即可，落地程序支持水平扩展，提高存储能力。
```