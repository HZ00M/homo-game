package com.homo.game.activity.core;

import com.alibaba.fastjson.annotation.JSONField;
import com.homo.core.utils.lang.KKMap;
import com.homo.core.utils.lang.KVData;
import com.homo.core.utils.rector.Homo;
import com.homo.game.activity.core.data.NodeData;
import com.homo.game.activity.core.point.DynamicBindPointProxy;
import com.homo.game.activity.facade.event.Event;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 数据所有者
 * 用于保存用户在所有节点（Node）相关的数据
 */
@Log4j2
public class Owner {
    /**
     * 所有者id
     * 存储时需要使用，标识该用户
     */
    String id;
    /**
     * 所有者类型
     * 存储时需要使用，表示处理的是哪种业务的数据
     */
    String type;
    /**
     * entity是否已经落地销毁
     */
    boolean isDestroy = false;
    /**
     * owner自身的数据，不属于任何一个node，是所有node都可以访问的
     */
    public KVData ownerData = new KVData();
    /**
     * owner消息绑定点列表（msgId-msgType-point）
     * node打开（open）时会将自身的绑定点添加到该owner上
     */
    protected Map<String, DynamicBindPointProxy> eventPoints = new HashMap<>();
    /**
     * 节点数据列表（address-nodeData）
     * SerializerFeature.WriteClassName 表示会保存子类的类型信息
     */
    protected Map<String, NodeData> nodeDataMap = new ConcurrentHashMap<>();
    /**
     * 根节点
     * 所有其他node都是根节点的子节点
     */
    @JSONField(deserialize = false, serialize = false)
    private Node rootNode;

    private NodeData rootNodeData = new NodeData();
    /**
     * owner加载器
     * 当本owner被定时器等异步过程持有,但实际已经在内存中销毁，那么就需要使用加载器重新加载出owner
     */
    @JSONField(deserialize = false, serialize = false)
    private OwnerLoader ownerLoader;//todo 异步加载数据

    /**
     * 重新加载数据
     * 当本owner被定时器等异步过程持有,但实际已经在内存中销毁，那么就需要使用加载器重新加载出owner
     *
     * @return
     */
    public Homo<Owner> reload() {
        return ownerLoader.asyncGet(this);
    }

    /**
     * 绑定需要处理某个事件的节点对象
     * 当节点被打开时会调用此函数
     */
    public void bindEventNode(String eventId,  NodeData nodeData, Integer order) {
        DynamicBindPointProxy dynamicBindPointProxy = eventPoints.get(eventId);
        if (dynamicBindPointProxy == null) {
            DynamicBindPointProxy bindPoint = new DynamicBindPointProxy();
            if (order != null) {
                bindPoint.bind(nodeData, order);
            } else {
                bindPoint.bindToTail(nodeData);
            }
            eventPoints.put(eventId, bindPoint);
        }
    }

    /**
     * 取消某个节点对象与事件的绑定
     * 取消绑定后，当收到匹配的事件后不再通知取消的node对象处理
     */
    public void unbindEventNode(String eventId, NodeData nodeData) {
        DynamicBindPointProxy dynamicBindPointProxy = eventPoints.get(eventId);
        dynamicBindPointProxy.unbind(nodeData);
    }

    /**
     * 事件处理入口，将事件分发到关注该事件的所有节点中
     * 如果没有一个节点关注该事件就将该关注点删除
     */
    public void onEvent(Event event) throws Exception {
        DynamicBindPointProxy bindPoint = eventPoints.get(event.getId());
        if (bindPoint != null) {
            if (bindPoint.isEmpty()) {
                eventPoints.remove(event.getId());//todo 这里需要验证bindPoint是否会被回收
            } else {
                bindPoint.broadcast(this, event);
            }
        }
    }

    /**
     * 重置数据
     */
    public void resetData(){
        eventPoints = new HashMap<>();
        ownerData = new KVData();
        nodeDataMap = new ConcurrentHashMap<>();
        //todo 打开根节点
    }

    /**
     * 设置实例数据
     *
     * @return
     */
    public NodeData setNodeData(String address, NodeData nodeData) {
        return nodeDataMap.put(address, nodeData);
    }

    /**
     * 获取实例数据
     *
     * @return
     */
    public NodeData getNodeData(String address) {
        return nodeDataMap.get(address);
    }

//    /**
//     * 创建并绑定节点数据
//     */
//    public NodeData createAndBindNodeData(Point point){
//        NodeData nodeData = getNodeData(point.getAddress());
//        if (nodeData == null){
//            nodeData = point.createNodeData(this);
//        }
//    }

    public void onDestroy() {
        //todo 落地
        isDestroy = true;
    }

    public boolean isDestroy() {
        return isDestroy;
    }

    public <T> T setValue(String key, T value) {
        if (value != null) {
            return ownerData.set(key, value);
        }
        return null;
    }

    public Integer getInt(String key, int defaultValue) {
        return getValue(key, defaultValue);
    }

    public String getString(String key, String defaultValue) {
        return getValue(key, defaultValue);
    }

    public Long getLong(String key, long defaultValue) {
        return getValue(key, defaultValue);
    }

    private <T> T getValue(String key, T defaultValue) {
        return (T) ownerData.data.getOrDefault(key, defaultValue);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setOwnerLoader(OwnerLoader ownerLoader) {
        this.ownerLoader = ownerLoader;
    }
}
