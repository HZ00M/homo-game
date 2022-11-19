package com.homo.game.activity.core.config;

import com.alibaba.fastjson.JSONObject;
import com.homo.core.utils.lang.KVData;
import com.homo.game.activity.core.Node;
import com.homo.game.activity.core.data.NodeData;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 节点配置信息
 * 包含节点类型，参数信息
 * 节点创建时必须传入一个节点配置
 */
@Data
public class NodeConfig {
    /**
     * 参数列表
     */
    public KVData params = new NodeData();

    /**
     * 节点类型Id (如果该子节点是个组合节点，则配置typeId,否则配置节点类型名（typeName）即可)
     * 可选配置，typeId和typeName必须配置一个
     * 和typeName是等价的，用来找到一个节点类型信息
     */
    public Integer typeId;

    /**
     * 节点类型名 (如果该子节点是个组合节点，则配置typeId,否则配置节点类型名（typeName）即可)
     * 可选配置，typeId和typeName必须配置一个
     * 和typeName是等价的，用来找到一个节点类型信息
     */
    public String typeName;

    /**
     * 在一个CombineConfig下唯一
     */
    public String indexId;

    /**
     * 分组ID
     * 一个活动下包含多个NodeConfig,每个NodeConfig会被创建成一个Node
     */
    public String activityId;

    /**
     * 节点tag
     * 在一个activityId下唯一
     * activityId + tag 可索引到一个唯一的node
     */
    public String tag;
    /**
     * 本节点publish端点
     */
    public List<String> publish = new ArrayList<>();
    /**
     * 本节点subscription端点
     */
    public List<String> subscription = new ArrayList<>();
    /**
     * 本节点ask端点
     */
    public List<String> ask = new ArrayList<>();
    /**
     * 本节点reply端点
     */
    public List<String> reply = new ArrayList<>();

    public NodeConfig(Integer typeId) {
        this.typeId = typeId;
    }
    public NodeConfig(String indexId, String typeName) {
        this.typeName = typeName;
        this.indexId = indexId;
    }

    public NodeConfig(String indexId, Integer typeId) {
        this.indexId = indexId;
        this.typeId = typeId;
    }

    public NodeConfig(String typeName) {
        this.typeName = typeName;
    }

    public NodeConfig(Class<? extends Node> nodeClazz){
        this.typeName = Node.getTypeName(nodeClazz);
    }

    public NodeConfig(String typeName, JSONObject params) {
        this(typeName);
        this.params.data = params;
    }

    public NodeConfig(String typeName, Map<String, Object> params) {
        this(typeName);
        this.params.data.putAll(params);
    }

    public NodeConfig buildParam(String paramName, Object value){
        this.params.set(paramName, value);
        return this;
    }

    public NodeConfig buildTag(String tag){
        this.tag = tag;
        return this;
    }

    public NodeConfig buildActivityId(String activityId){
        this.activityId = activityId;
        return this;
    }
}
