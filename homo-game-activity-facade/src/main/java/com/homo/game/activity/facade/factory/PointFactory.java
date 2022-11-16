package com.homo.game.activity.facade.factory;


import com.homo.game.activity.facade.Point;

/**
 * 负责节点的创建和管理
 */
public interface PointFactory {

    /**
     * 通过地址获取一个节点实例
     */
     Point getPoint(String address);
}
