package com.homo.game.activity.core;

import com.homo.core.utils.delegate.Delegate2PR;
import com.homo.core.utils.delegate.Delegate2PVoid;
import com.homo.core.utils.rector.Homo;
import com.homo.game.activity.core.data.NodeData;
import com.homo.game.activity.facade.Point;
import com.homo.game.activity.facade.annotation.BindEvent;
import com.homo.game.activity.core.compoment.Component;
import com.homo.game.activity.facade.event.*;
import com.homo.game.activity.facade.factory.PointFactory;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.CollectionUtils;
import reactor.util.function.Tuple2;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点（Node及其子类），活动系统的核心
 * <p>
 * 包含节点的核心逻辑实现
 * 包括发布、订阅、请求、响应、事件处理的实现
 * 所有节点都在程序初始化的时候一次性创建好，并且同时建立好节点之间的关系
 * 节点的动态数据都保存在某个用户（Owner）身上，节点本身是无状态的
 * 节点的open，是指在某个用户（Owner）上打开该节点，打开时会再Owner上生成该节点相关的数据（NodeData）,
 * 同时会将Owner相关的外部事件（ReportEvent）处理列表添加该节点的订阅
 * 节点的close，是指在某个用户（Owner）上关闭该节点，关闭时会清除Owner上该节点对应的数据（NodeData）,
 * 同时会将Owner相关的外部事件（ReportEvent）处理列表中去掉该节点的订阅
 * </p>
 * 如果Owner上某个节点没有被打开，那么该Owner相关的事件（ReportEvent）就不会被该节点订阅处理
 * Owner相关的外部请求事件（RequestEvent）的处理不依赖与节点的open，节点都可以处理Owner的外部请求事件（RequestEvent）
 */
@Log4j2
@Data
public abstract class Node implements Point {

    /**
     * 节点工厂
     * 负责节点的创建和管理，所有节点实例都保存在工厂中，通过address获取
     */
    public static PointFactory pointFactory;
    /**
     * 节点类型名缓存
     */
    public static Map<Class<?>, String> typeNameCacheMap = new HashMap<>();

    /**
     * 节点状态
     * 保存在Owner上本节点对应的NodeData中
     * 表示Owner上本节点当前的状态
     */
    public enum State {
        /**
         * 未开启
         * 节点还没有调用open，此时节点还不会订阅外部上报事件（ReportEvent），该节点的数据（NodeData）还未创建
         */
        unOpen,
        /**
         * 开启
         * 节点open函数被调用，此时节点订阅外部上报事件（ReportEvent），并且该节点的数据（NodeData）应该已经从Owner上创建
         */
        open,
        /**
         * 已关闭
         * 节点close函数被调用，此时节点还不会订阅外部上报事件（ReportEvent），并且该节点的数据（NodeData）应该已经从Owner上删除
         */
        close
    }

    /**
     * 节点类型名
     * 对于java实现的基础节点，节点类型名就是本java对象的简单类名
     */
    public String typeName;

    /**
     * 标签，该节点实例的别名，客户端可以通过tag + activityId索引到此node
     */
    public String tag;

    /**
     * 分组id，同一个活动的所有节点的activityId相同。客户端可以通过tag + activityId索引到此node
     */
    public String activityId;

    /**
     * 节点id，本质上是子节点在父节点的索引值，一个父节点下的直接子节点id是不重复的
     */
    public String indexId;

    /**
     * 节点路径
     * 本质上是父节点的索引地址（address）
     * path + indexId = 本节点的地址（address）
     */
    public String path;

    /**
     * 本节点地址，所有节点唯一不重复
     */
    public String address;

    /**
     * 父节点
     */
    public Node parent;

    /**
     * 子节点列表
     */
    public Map<String, Node> childMap;

    /**
     * 子节点typeName->子节点实例列表
     */
    public Map<String, List<Node>> typeToChildNodes = new HashMap<>();

    /**
     * 组件列表
     * （组件的功能合节点基本相同，节点可通过绑定不同组件拥有不同组件的能力）
     */
    public Map<String, Component> componentMap;

    /**
     * 本节点响应外部请求端点列表
     */
    public Map<String, Delegate2PR<NodeData, Event, Homo<ReportEvent>>> replayPoints = new HashMap<>();

    /**
     * 本节点向外发出请求端点列表
     */
    public Map<String, Delegate2PR<NodeData, RequestEvent, Homo<ResponseEvent>>> askPoints = new HashMap<>();

    /**
     * 本节点订阅事件端点列表（可以是外部上报事件ReportEvent，也可以是内部传播事件InnerEvent）
     */
    public Map<String, Delegate2PVoid<NodeData, Event>> subPoints = new HashMap<>();

    /**
     * 本节点向外发布的事件端点列表
     */
    public Map<String, Delegate2PVoid<NodeData, Event>> pubPoints = new HashMap<>();

    /**
     * 通过typeName获取一个Component
     */
    public Component getComponent(String typeName) {
        return componentMap.getOrDefault(typeName, null);
    }

    /**
     * 通过指定类型获取Component
     */
    public <T extends Component> T getComponent(Class<T> componentClass) {
        return (T) getComponent(getTypeName(componentClass));
    }

    /**
     * 获取节点类型名
     *
     * @param clazz 节点类
     */
    public static String getTypeName(Class<?> clazz) {
        return typeNameCacheMap.computeIfAbsent(clazz, Class::getSimpleName);
    }

    /**
     * 通过注解构造需要处理的上报事件列表
     * 在open时会绑定到owner上，close时会取消绑定
     *
     * @param point 事件处理对象，可能是node也可能是component
     * @param clazz 事件处理对象类型
     */
    protected void initBindEvent(Point point, Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            BindEvent bindEvent = method.getAnnotation(BindEvent.class);
            if (bindEvent != null) {
                String msgId;
                Class<?> eventClazz = void.class.equals(bindEvent.value()) ? method.getParameterTypes()[1] : bindEvent.value();
                if (bindEvent.msgId().equals("")) {
                    msgId = Node.getTypeName(eventClazz);
                } else {
                    msgId = bindEvent.msgId();
                }
                method.setAccessible(true);
                registerSubFun(msgId, bindEvent.order(), (nodeData, event) -> {
                    if (!bindEvent.type().equals(EvenType.any)) {
                        if (!event.getType().equals(bindEvent.type())) {
                            return;
                        }
                    }
                    try {
                        if (Homo.class.isAssignableFrom(method.getReturnType())) {
                            ((Homo<Void>) method.invoke(point, nodeData, event)).start();
                        } else {
                            method.invoke(point, nodeData, event);
                        }
                    } catch (Exception e) {
                        log.error("node {} process event {} {} by method {} error", getAddress(), Node.getTypeName(event.getClass()), event.getType(), method.getName());
                    }
                });
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            initBindEvent(point, superclass);
        }
    }

    /**
     * 注册订阅事件
     *
     * @param msgId
     * @param order
     * @param subFun
     */
    public void registerSubFun(String msgId, int order, Delegate2PVoid.ExecuteFun<NodeData, Event> subFun) {
        subPoints.computeIfAbsent(msgId, k -> new Delegate2PVoid<>()).bind(subFun, order);
    }

    public Delegate2PVoid<NodeData, Event> getSubPoint(String msgId) {
        return subPoints.get(msgId);
    }

    /**
     * 分发事件
     */
    public void dispatchEvent(NodeData nodeData, Event event) {
        Delegate2PVoid<NodeData, Event> subPoint = getSubPoint(event.getId());
        if (subPoint != null) {//该节点有订阅该事件的端点，分发事件
            subPoint.publish(nodeData, event);
            processPublish(nodeData,event);
        } else {
            log.info("dispatchEvent node {} not subPoint msgId {}", Node.getTypeName(this.getClass()), event.getId());
        }
    }

    /**
     * 该节点待发布的事件列表
     */
    public ThreadLocal<List<Tuple2<String,Event>>> threadLocalPrePubList = new InheritableThreadLocal<>();

    public List<Tuple2<String,Event>> popPubList(){
        List<Tuple2<String, Event>> list = threadLocalPrePubList.get();
        if (list != null) {
            threadLocalPrePubList.set(null);
        }
        return list;
    }

    /**
     * 处理该节点pub事件
     */
    public void processPublish(NodeData nodeData,Event event){
        List<Tuple2<String, Event>> pubList = popPubList();
        if (!CollectionUtils.isEmpty(pubList)){
            pubList.forEach(item->{
                pubPoints.computeIfPresent(item.getT1(),(msgId,pubPoint)->{
                    pubPoint.publish(nodeData,item.getT2());
                    return pubPoint;
                });
            });
        }
    }


}
