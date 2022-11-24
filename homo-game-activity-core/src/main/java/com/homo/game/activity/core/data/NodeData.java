package com.homo.game.activity.core.data;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.homo.core.utils.lang.KVData;
import com.homo.game.activity.core.Node;
import com.homo.game.activity.core.Owner;
import com.homo.game.activity.core.Point;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点动态信息
 * 用户在该node下的信息载体，存在在owner上
 */
@Data
public class NodeData extends KVData {
    public boolean isOpen;
    public String address;
    public String tag;
    @JSONField(serialzeFeatures = SerializerFeature.WriteClassName)
    public Map<String, NodeData> childData = new HashMap<>();
    @JSONField(deserialize = false, serialize = false)
    public Owner owner;//用于双向绑定
    @JSONField(deserialize = false, serialize = false)
    public NodeData parentData;//用于双向绑定
    @JSONField(deserialize = false, serialize = false)
    public Point point;//该NodeData所属的point，可能是node也可能是component
    @JSONField(deserialize = false, serialize = false)
    public Node mainNode;//node和component都是获取宿主节点

}
