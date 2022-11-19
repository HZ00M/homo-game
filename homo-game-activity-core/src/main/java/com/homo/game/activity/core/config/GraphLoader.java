package com.homo.game.activity.core.config;

import java.util.List;

/**
 * 组合节点加载器接口
 */
public interface GraphLoader {
    /**
     * 返回所有组合节点信息列表
     */
    List<CombineConfig> getCombines();
}
