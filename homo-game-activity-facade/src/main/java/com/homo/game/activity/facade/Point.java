package com.homo.game.activity.facade;

public interface Point {

    /**
     * 节点唯一id
     * point的数据是存在owner上的，通过地址索引得到
     * node和component实例都有自己的地址，通过这个地址可以从owner中拿到自己的数据
     * @return
     */
    String getAddress();
}
