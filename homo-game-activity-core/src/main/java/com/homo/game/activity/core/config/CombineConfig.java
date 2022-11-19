package com.homo.game.activity.core.config;

import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组合节点配置
 * 通常由一个表格或一个json生成
 * 包含父节点的基础类型，参数列表，子节点的配置列表，子节点间的关系
 * <p>
 * 一个组合节点默认会有一个父节点（FatherNode）、一个输入节点（InSideCar）、一个输出节点（OutSideCar）及其他子节点构成
 */
public class CombineConfig {
    /**
     * 组合的父类型名（必选，FatherNode或其子类）
     */
    public String fatherType;

    /**
     * 节点类型名（必选）不可重复
     * 通过类型名可查找到一个唯一的NodeType
     */
    public String typeName;

    /**
     * 节点类型id（可选）不可重复
     * 通过节点类型id可查找到一个唯一的NodeType
     */
    public Integer typeId;

    /**
     * 分组id
     * 多个combineConfig的activityId如果相同，代表他们属于同一个分组，分组事件可以在同一个activityId的组合节点间传播
     * combineConfig下的所有NodeConfig都具有相同的activityId
     */
    public String activityId;
    /**
     * 参数信息(FatherNode持有)
     * 存储所有参数的名字和类型
     */
    public Map<String, String> params = new HashMap<>();

    /**
     * 组合节点需要对外发布的事件名称列表（InSideCar持有）
     */
    public List<String> publish = new ArrayList<>();
    /**
     * 组合节点需要订阅的外部事件名称列表（OutSideCar持有）
     */
    public List<String> subscription = new ArrayList<>();
    /**
     * 组合节点需要对外请求的事件名称列表（OutSideCar持有）
     */
    public List<String> ask = new ArrayList<>();
    /**
     * 组合节点需要响应的外部请求的事件名称列表（InSideCar持有）
     */
    public List<String> reply = new ArrayList<>();

    public CombineConfig(String typeName, String fatherType) {
        this.typeName = typeName;
        this.fatherType = fatherType;
    }

    /**
     * 连接信息类
     * 保存一个节点与其他节点的连接关系
     */
    public static class Connect {
        /**
         * 1-1  askPoint-(address-replyPoint)
         * 请求响应关系
         */
        public Map<String, Tuple2<String, String>> askTo = new HashMap<>();
        /**
         * 1-n pubPoint-(address-subPoint)
         * 发布订阅关系
         */
        public Map<String, Map<String, String>> pubTo = new HashMap<>();
    }

    /**
     * (configId-connect)
     * 子节点连接关系列表
     */
    public Map<String, Connect> connects = new HashMap<>();

    /**
     * (configId-NodeConfig)
     * 子节点配置信息列表
     */
    public Map<String,NodeConfig> childNodeConfigs = new HashMap<>();

    /**
     * 添加子节点
     */
    public CombineConfig addNode(String id,NodeConfig nodeConfig){
        childNodeConfigs.put(id,nodeConfig);
        return this;
    }
}
