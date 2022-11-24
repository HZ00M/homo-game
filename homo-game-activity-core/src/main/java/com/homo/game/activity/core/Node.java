package com.homo.game.activity.core;

import com.homo.core.utils.delegate.Delegate2PR;
import com.homo.core.utils.delegate.Delegate2PVoid;
import com.homo.core.utils.exception.HomoError;
import com.homo.core.utils.rector.Homo;
import com.homo.game.activity.core.compoment.BindComponent;
import com.homo.game.activity.core.compoment.Component;
import com.homo.game.activity.core.config.CombineConfig;
import com.homo.game.activity.core.config.NodeConfig;
import com.homo.game.activity.core.data.NodeData;
import com.homo.game.activity.core.exception.HomoActivityError;
import com.homo.game.activity.core.factory.NodeFactory;
import com.homo.game.activity.core.node.FatherNode;
import com.homo.game.activity.core.node.InSideCarNode;
import com.homo.game.activity.core.node.OutSideCarNode;
import com.homo.game.activity.core.point.*;
import com.homo.game.activity.facade.annotation.*;
import com.homo.game.activity.facade.component.Single;
import com.homo.game.activity.facade.event.Event;
import com.homo.game.activity.facade.event.EventType;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

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

    public static String SELF_ID = "Self";

    public static String PARENT_ID = "Parent";
    /**
     * 节点工厂
     * 负责节点的创建和管理，所有节点实例都保存在工厂中，通过address获取
     */
    public static NodeFactory nodeFactory;
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
     * 如果该节点是当前组合节点的父节点，则它将拥有边车节点
     */
    protected InSideCarNode inSideCarNode;
    /**
     * 如果该节点是当前组合节点的父节点，则它将拥有边车节点
     */
    protected OutSideCarNode outSideCarNode;

    /**
     * indexId-Node
     * 子节点列表
     */
    public Map<String, Node> childMap;

    /**
     * 子节点typeName->子节点实例列表
     */
    public Map<String, List<Node>> typeToChildNodes = new HashMap<>();

    /**
     * (componentTypeName-component)
     * 组件列表
     * （组件的功能合节点基本相同，节点可通过绑定不同组件拥有不同组件的能力）
     */
    public Map<String, List<Component>> componentMap = new HashMap<>();

    /**
     * 组合节点的配置信息
     * 包括公用参数，对外端点，子节点配置，子节点连接信息
     */
    protected CombineConfig combineConfig;

    /**
     * 该节点的配置信息
     */
    public NodeConfig nodeConfig;

    /**
     * 本节点响应外部请求端点列表
     */
    public Map<String, ReplyPoint<Homo<Event>>> replyPoints = new HashMap<>();

    /**
     * 本节点向外发出请求端点列表
     */
    public Map<String, AskPoint<Event, Event>> askPoints = new HashMap<>();

    /**
     * pointName->subPoint
     * 本节点订阅端点列表
     */
    public Map<String, SubPoint> subPoints = new HashMap<>();

    /**
     * eventId-eventPoint
     * 本节点订阅事件端点列表（可以是外部上报事件ReportEvent，也可以是内部传播事件InnerEvent）
     */
    public Map<String, EventPoint> eventPoints = new HashMap<>();

    /**
     * 本节点向外发布的事件端点列表
     */
    public Map<String, PubPoint<Event>> pubPoints = new HashMap<>();


    /**
     * 通过typeName获取一个Component
     */
    public Component getComponent(String typeName) {
        List<Component> components = componentMap.get(typeName);
        if (!CollectionUtils.isEmpty(components)) {
            return components.get(0);
        } else {
            return null;
        }
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
                registerSubFun(msgId, bindEvent.type(), bindEvent.order(), (nodeData, event) -> {
                    if (!bindEvent.type().equals(EventType.any)) {//如果是any类型则直接调用，否则需要判断类型
                        if (!event.getType().equals(bindEvent.type())) {
                            log.warn("event type {} not match method bind type {}", event.getType(), bindEvent.type());
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
     */
    public void registerSubFun(String msgId, EventType eventType, int order, Delegate2PVoid.ExecuteFun<NodeData, Event> subFun) {
        EventPoint eventPoint = eventPoints.get(msgId);
        if (eventPoint == null) {
            eventPoint = new EventPoint(msgId, eventType);
            eventPoints.put(msgId, eventPoint);
        }
        eventPoint.bind(subFun, order);
        //todo 绑定事件
    }

    /**
     * 分发事件
     */
    public void dispatchEvent(NodeData nodeData, Event event) {
        SubPoint subPoint = subPoints.get(event.getId());
        if (subPoint != null) {//该节点有订阅该事件的端点，分发事件
            subPoint.publish(nodeData, event);
            processPublish(nodeData);
        } else {
            log.info("dispatchEvent node {} not subPoint msgId {}", Node.getTypeName(this.getClass()), event.getId());
        }
    }

    /**
     * 该节点待发布的事件列表
     */
    public ThreadLocal<List<Tuple2<String, Event>>> threadLocalPrePubList = new InheritableThreadLocal<>();

    public <T extends Event> void publish(String pointName, T event) {
        List<Tuple2<String, Event>> prePubList = getPrePubList();
        prePubList.add(Tuples.of(pointName, event));
    }

    public List<Tuple2<String, Event>> getPrePubList() {
        if (threadLocalPrePubList.get() == null) {
            threadLocalPrePubList.set(new LinkedList<>());
        }
        return threadLocalPrePubList.get();
    }

    public List<Tuple2<String, Event>> popPubList() {
        List<Tuple2<String, Event>> list = threadLocalPrePubList.get();
        if (list != null) {
            threadLocalPrePubList.set(null);
        }
        return list;
    }

    /**
     * 处理该节点pub事件
     */
    public void processPublish(NodeData nodeData) {
        List<Tuple2<String, Event>> pubList = popPubList();
        if (!CollectionUtils.isEmpty(pubList)) {
            pubList.forEach(item -> {
                pubPoints.computeIfPresent(item.getT1(), (msgId, pubPoint) -> {
                    pubPoint.publish(nodeData, item.getT2());
                    return pubPoint;
                });
            });
        }
    }

    /**
     * 将该节点关注的事件绑定到owner的事件关注点
     */
    public void bindSubEvent(NodeData nodeData) {
        Collection<EventPoint> pointsAll = eventPoints.values();
        pointsAll.forEach(point -> {
            nodeData.getOwner().bindEventNode(point.eventId, point.eventType, nodeData, null);//默认添加到对尾
        });
    }

    /**
     * 将该节点关注的事件从owner上解除绑定
     */
    public void unbindSubEvent(NodeData nodeData) {
        Collection<EventPoint> pointsAll = eventPoints.values();
        pointsAll.forEach(point -> {
            nodeData.getOwner().unbindEventNode(point.eventId, point.eventType, nodeData);
        });
    }

    /**
     * 根据子节点id获取子节点对象
     * <p>
     * id = "Father"返回自身
     * id = "In"返回输入代理节点
     * id = "Out"返回输出代理节点
     * </P>
     */
    public Node getChildById(String id) {
        if (Node.SELF_ID.equals(id)) {
            return this;
        }
        if (InSideCarNode.ID.equals(id)) {
            return inSideCarNode;
        }
        if (OutSideCarNode.ID.equals(id)) {
            return outSideCarNode;
        }
        if (Node.PARENT_ID.equals(id)) {
            return parent;
        }
        Node node = childMap.get(id);
        return node;
    }

    public void init(Node parent, String indexInParent, NodeConfig nodeConfig, CombineConfig combineConfig) {
        initBaseInfo(parent, indexInParent, nodeConfig, combineConfig);
        initParams(this, getClass());
        initBindEvent(this, getClass());
        initPubAndAsk(this, getClass());
        initSubAndReply(this, getClass());
        initComponent(this, getClass());
        if (FatherNode.class.isAssignableFrom(this.getClass()) && combineConfig != null) {
            FatherNode fatherNode = (FatherNode) this;
            initOutSideCar(fatherNode);
            initInSideCar(fatherNode);
            initChildNode(fatherNode);
            initConnect(fatherNode);
        }
    }

    protected void initConnect(FatherNode fatherNode) {
        combineConfig.connects.forEach((srcId, connect) -> {
            //获得出口节点
            Node srcNode = getChildById(srcId);
            connect.askTo.forEach((srcPointName, tuple) -> {
                //获得入口端点
                String address = tuple.getT1();
                String descPointName = tuple.getT2();
                Node descNode = getChildById(address);
                srcNode.askConnectReply(srcNode, srcPointName, descNode, descPointName);
            });
            connect.pubTo.forEach((srcPointName,map)->{
                map.forEach((descAddress,subPointName)->{
                    //获得入口端点
                    Node descNode = getChildById(descAddress);
                    srcNode.pubConnectSub(srcNode,srcPointName,descNode,subPointName);
                });
            });
        });
    }

    protected void pubConnectSub(Node srcNode, String srcPointName, Node descNode, String descPointName) {
        PubPoint<Event> pubPoint = srcNode.pubPoints.get(srcPointName);
        if (pubPoint == null){
            log.error("pubConnectSub srcNode {} descNode {} srcPointName {} descPointName {} not exist ",srcNode,descNode,srcPointName,descPointName);
        }else {
            pubPoint.append(descNode.subPoints.get(descPointName));
        }
    }

    protected void askConnectReply(Node srcNode, String srcPointName, Node descNode, String descPointName) {
        AskPoint<Event, Event> askPoint = srcNode.askPoints.get(srcPointName);
        if (askPoint == null){
            log.error("askConnectReply srcNode {} descNode {} srcPointName {} descPointName {} not exist ",srcNode,descNode,srcPointName,descPointName);
        }else {
            askPoint.append(descNode.replyPoints.get(descPointName));
        }
    }

    protected void initChildNode(FatherNode fatherNode) {
        combineConfig.childNodeConfigs.forEach((configId, nodeConfig) -> {
            NodeFactory.newNode(fatherNode, configId, nodeConfig);
        });
    }

    protected void initInSideCar(FatherNode fatherNode) {
        inSideCarNode = (InSideCarNode) NodeFactory.newNode(fatherNode, InSideCarNode.ID, new NodeConfig(Node.getTypeName(InSideCarNode.class)));
        //父节点的ask要暴露到外部获取外部的值
        Set<String> askPointSet = new HashSet<>(this.askPoints.keySet());
        //编辑器动态添加的ask也需要暴露到外部获取外部的值
        askPointSet.addAll(this.combineConfig.ask);
        //将内部的请求转发到外部
        askPointSet.forEach(pointName -> {
            Delegate2PR.ExecuteFun<NodeData, Event, Homo<Event>> replyFun = new Delegate2PR.ExecuteFun<NodeData, Event, Homo<Event>>() {
                @Override
                public Homo<Event> apply(NodeData nodeData, Event event) throws Exception {
                    AskPoint<Event, Event> askPoint = inSideCarNode.askPoints.get(pointName);
                    if (askPoint == null) {
                        return Homo.result(null);
                    }
                    return askPoint.publish(nodeData, event);
                }
            };
            //内部的ask可以通过inSideCarNode的reply获取值
            inSideCarNode.replyPoints.computeIfAbsent(pointName, s -> new ReplyPoint(inSideCarNode, pointName)).bindToTail(replyFun);
            //暴露ask到外部获取外部的值
            inSideCarNode.askPoints.put(pointName, new AskPoint(inSideCarNode, pointName));
        });
        //对外部的sub转化为对内部的pub
        Set<String> subPointSet = new HashSet<>(this.subPoints.keySet());
        //编辑器动态添加的sub也需要pub到内部
        subPointSet.addAll(this.combineConfig.subscription);
        subPointSet.forEach(pointName -> {
            Delegate2PVoid.ExecuteFun<NodeData, Event> subFun = new Delegate2PVoid.ExecuteFun<NodeData, Event>() {
                @Override
                public void run(NodeData nodeData, Event event) throws Exception {
                    PubPoint<Event> pubPoint = inSideCarNode.pubPoints.get(pointName);
                    if (pubPoint == null) {
                        return;
                    }
                    pubPoint.publish(event);
                }
            };
            inSideCarNode.subPoints.computeIfAbsent(pointName, s -> new SubPoint(inSideCarNode, pointName)).bindToHead(subFun);//todo 这里需要验证
            inSideCarNode.pubPoints.put(pointName, new PubPoint(inSideCarNode, pointName));
        });
    }

    /**
     * 初始化类型数据，添加所有子节点
     */
    protected void initOutSideCar(FatherNode fatherNode) {
        outSideCarNode = (OutSideCarNode) NodeFactory.newNode(fatherNode, OutSideCarNode.ID, new NodeConfig(Node.getTypeName(OutSideCarNode.class)));
        //父节点的reply代表需要暴露给外部
        Set<String> pointSet = new HashSet<>(this.replyPoints.keySet());
        //编辑器动态添加的reply也需要暴露给外部
        pointSet.addAll(this.combineConfig.reply);
        //将外部的请求转发到内部
        pointSet.forEach(pointName -> {
            Delegate2PR.ExecuteFun<NodeData, Event, Homo<Event>> replyFun = new Delegate2PR.ExecuteFun<NodeData, Event, Homo<Event>>() {
                @Override
                public Homo<Event> apply(NodeData nodeData, Event event) throws Exception {
                    Delegate2PR<NodeData, Event, Homo<Event>> askPoint = outSideCarNode.askPoints.get(pointName);
                    if (askPoint == null) {
                        return Homo.result(null);
                    }
                    return askPoint.publish(nodeData, event);
                }
            };
            //将外部需要的reply转换为对内部的ask用以从内部获取值
            outSideCarNode.replyPoints.computeIfAbsent(pointName, s -> new ReplyPoint(outSideCarNode, pointName)).bindToTail(replyFun);
            //转换成OutSideCar向内部的ask
            outSideCarNode.askPoints.put(pointName, new AskPoint(outSideCarNode, pointName));
        });

        //将内部的publish暴露给外部sub
        Set<String> pubPointSet = new HashSet<>(this.pubPoints.keySet());
        //编辑器动态添加的pub也需要sub到内部
        pubPointSet.addAll(this.combineConfig.subscription);
        pubPointSet.forEach(pointName -> {
            Delegate2PVoid.ExecuteFun<NodeData, Event> subFun = new Delegate2PVoid.ExecuteFun<NodeData, Event>() {
                @Override
                public void run(NodeData nodeData, Event event) throws Exception {
                    PubPoint<Event> pubPoint = inSideCarNode.pubPoints.get(pointName);
                    if (pubPoint == null) {
                        return;
                    }
                    pubPoint.publish(event);
                }
            };
            outSideCarNode.subPoints.computeIfAbsent(pointName, s -> new SubPoint(outSideCarNode, pointName)).bindToHead(subFun);//todo 这里需要验证
            outSideCarNode.pubPoints.put(pointName, new PubPoint(outSideCarNode, pointName));
        });
    }


    protected void initComponent(Node hostNode, Class<?> hostClazz) {
        /**
         * 将声明的Component属性实例化
         */
        for (Field field : hostClazz.getDeclaredFields()) {
            if (Component.class.isAssignableFrom(field.getType())) {
                Component component = createAndBindComponent(hostNode, (Class<? extends Component>) field.getType());
                try {
                    field.setAccessible(true);
                    field.set(hostNode, component);
                } catch (Exception e) {
                    throw HomoError.throwError(HomoActivityError.initComponent, hostNode.address, field.getType());
                }
            }
        }
        /**
         * 将注解上的Component实例化
         */
        BindComponent bindComponent = hostClazz.getAnnotation(BindComponent.class);
        if (bindComponent != null) {
            for (Class<? extends Component> componentClazz : bindComponent.value()) {
                createAndBindComponent(hostNode, componentClazz);
            }
        }
        Class<?> superclass = hostClazz.getSuperclass();
        if (superclass != null) {
            initComponent(hostNode, superclass);
        }
    }


    protected Component createAndBindComponent(Node hostNode, Class<? extends Component> componentClazz) {
        Component component = null;
        try {
            component = componentClazz.newInstance();
            component.setNode(hostNode);
            initParams(component, component.getClass());
            initBindEvent(component, component.getClass());
            initPubAndAsk(component, component.getClass());
            initSubAndReply(component, component.getClass());
        } catch (Exception e) {
            log.error("createComponent host {} component {}", hostNode.getAddress(), componentClazz);
            System.exit(-1);
        }
        hostNode.componentMap.computeIfAbsent(getTypeName(componentClazz), componentName -> new ArrayList<>()).add(component);
        if (component instanceof Single) {
            NodeFactory.addSingle((Single) component);
        }
        return component;
    }

    protected void initSubAndReply(Point point, Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            Reply reply = method.getAnnotation(Reply.class);
            if (reply != null) {
                method.setAccessible(true);
                String pointName = method.getName();
                if (!StringUtils.isEmpty(reply.value())) {
                    pointName = reply.value();
                }
                Class<?> returnType = method.getReturnType();
                //返回类型统一为Homo<Event>
                Delegate2PR.ExecuteFun<NodeData, Event, Homo<Event>> replyFun = null;
                if (Event.class.isAssignableFrom(returnType)) {
                    replyFun = new Delegate2PR.ExecuteFun<NodeData, Event, Homo<Event>>() {
                        @Override
                        public Homo<Event> apply(NodeData nodeData, Event event) throws Exception {
                            Event rel = (Event) method.invoke(point, point.getPointData(nodeData.owner), event);
                            return Homo.result(rel);
                        }
                    };
                } else if (Homo.class.isAssignableFrom(returnType)) {
                    replyFun = new Delegate2PR.ExecuteFun<NodeData, Event, Homo<Event>>() {
                        @Override
                        public Homo<Event> apply(NodeData nodeData, Event event) throws Exception {
                            Homo<Event> rel = (Homo<Event>) method.invoke(point, point.getPointData(nodeData.owner), event);
                            rel.nextValue(ret -> {
                                //可能产生新的Pub事件
                                processPublish(point.getPointData(nodeData.owner));
                                return ret;
                            });
                            return rel;
                        }
                    };
                }
                ReplyPoint replyPoint = new ReplyPoint(this, pointName);
                replyPoint.bindToTail(replyFun);
                if (point instanceof Component){//如果是组件则绑定到宿主节点上
                    Component component = (Component) point;
                    component.getNode().replyPoints.put(pointName, replyPoint);
                }else {
                    replyPoints.put(pointName, replyPoint);
                }

            }

            Subscription subscription = method.getAnnotation(Subscription.class);
            if (subscription != null) {
                method.setAccessible(true);
                String pointName = method.getName();
                if (!StringUtils.isEmpty(reply.value())) {
                    pointName = subscription.value();
                }
                Delegate2PVoid.ExecuteFun<NodeData, Event> subFun = new Delegate2PVoid.ExecuteFun<NodeData, Event>() {
                    @Override
                    public void run(NodeData nodeData, Event event) throws Exception {
                        method.invoke(point, point.getPointData(nodeData.owner), event);
                        processPublish(point.getPointData(nodeData.owner));
                    }
                };
                String finalPointName = pointName;
                if (point instanceof Component){//如果是组件则绑定到宿主节点上
                    Component component = (Component) point;
                    component.getNode().subPoints.computeIfAbsent(pointName, s -> new SubPoint(point, finalPointName)).bind(subFun, subscription.order());
                }else {
                    subPoints.computeIfAbsent(pointName, s -> new SubPoint(point, finalPointName)).bind(subFun, subscription.order());
                }
            }
        }
    }

    /**
     * 初始化Publish和Ask
     */
    protected void initPubAndAsk(Point point, Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getType().isAssignableFrom(PubPoint.class)) {
                String pointName = field.getName();
                Publish publish = field.getAnnotation(Publish.class);
                if (publish != null) {
                    pointName = publish.value();
                }
                try {
                    PubPoint pubPoint = new PubPoint(point, pointName);
                    field.set(point, pubPoint);
                    if (point instanceof Component){//如果是组件则绑定到宿主节点上
                        Component component = (Component) point;
                        component.getNode().pubPoints.put(pointName, pubPoint);
                    }else {
                        this.pubPoints.put(pointName, pubPoint);
                    }
                } catch (IllegalAccessException e) {
                    throw HomoError.throwError(HomoActivityError.initPubAndAsk, pointName);
                }
            }
            if (field.getType().isAssignableFrom(AskPoint.class)) {
                String pointName = field.getName();
                Ask ask = field.getAnnotation(Ask.class);
                if (ask != null) {
                    pointName = ask.value();
                }
                try {
                    AskPoint askPoint = new AskPoint(point, pointName);
                    field.set(point, askPoint);
                    if (point instanceof Component){//如果是组件则绑定到宿主节点上
                        Component component = (Component) point;
                        component.getNode().askPoints.put(pointName, askPoint);
                    }else {
                        askPoints.put(pointName, askPoint);
                    }

                } catch (IllegalAccessException e) {
                    throw HomoError.throwError(HomoActivityError.initPubAndAsk, pointName);
                }
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            initPubAndAsk(point, superclass);
        }
    }

    /**
     * 初始化节点参数，配置表参数优先与注解上的默认值
     *
     * @param point
     * @param clazz
     */
    protected void initParams(Point point, Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Config config = field.getAnnotation(Config.class);
            if (config != null) {
                Object v = nodeConfig.params.get(field.getName());
                try {
                    if (v != null) {//优先读取配置文件的值
                        if (v instanceof String) {
                            field.set(point, stringCastBaseType(v, field.getType()));
                        } else if (v instanceof Long) {
                            field.set(point, longCastBaseType(v, field.getType()));
                        } else if (v instanceof Integer) {
                            field.set(point, intCastBaseType(v, field.getType()));
                        } else {
                            throw HomoError.throwError(HomoActivityError.paramCastError, field.getName(), field.getType(), v.getClass().getTypeName());
                        }
                    } else if (!StringUtils.isEmpty(config.value())) {//读取注解上的默认值
                        field.set(point, stringCastBaseType(config.value(), field.getType()));
                    }
                } catch (Exception e) {
                    throw HomoError.throwError(HomoActivityError.initParams, field.getName());
                }
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            initParams(point, superclass);
        }
    }

    <T> T stringCastBaseType(Object obj, Class<T> tClass) {
        String value = (String) obj;
        if (String.class.equals(tClass)) {
            return (T) value;
        } else if (Integer.class.equals(tClass)) {
            return (T) Integer.valueOf(value);
        } else if (Long.class.equals(tClass)) {
            return (T) Long.valueOf(value);
        } else if (Boolean.class.equals(tClass)) {
            return (T) Boolean.valueOf(value);
        } else {
            log.error("不支持的转换类型 {}", tClass.getSimpleName());
            System.exit(-1);
            return null;
        }
    }

    <T> T longCastBaseType(Object obj, Class<T> tClass) {
        Long value = (Long) obj;
        if (String.class.equals(tClass)) {
            return (T) String.valueOf(value);
        } else if (Integer.class.equals(tClass)) {
            return (T) Integer.valueOf(value.intValue());
        } else if (Long.class.equals(tClass)) {
            return (T) value;
        } else if (Boolean.class.equals(tClass)) {
            return (T) Boolean.valueOf(value > 0);
        } else {
            log.error("不支持的转换类型 {}", tClass.getSimpleName());
            System.exit(-1);
            return null;
        }
    }

    <T> T intCastBaseType(Object obj, Class<T> tClass) {
        Integer value = (Integer) obj;
        if (String.class.equals(tClass)) {
            return (T) String.valueOf(value);
        } else if (Long.class.equals(tClass)) {
            return (T) Long.valueOf(value.longValue());
        } else if (Integer.class.equals(tClass)) {
            return (T) value;
        } else if (Boolean.class.equals(tClass)) {
            return (T) Boolean.valueOf(value > 0);
        } else {
            log.error("不支持的转换类型 {}", tClass.getSimpleName());
            System.exit(-1);
            return null;
        }
    }

    @Override
    public NodeData getPointData(Owner owner) {
        return null;
    }

    public Integer getIntParam(String key) {
        return getParamValue(key, Integer.class);
    }

    public String getStringParam(String key) {
        return getParamValue(key, String.class);
    }

    public Long getLongParam(String key) {
        return getParamValue(key, Long.class);
    }

    public <T> T getParamValue(String key, Class<T> tClass) {
        return nodeConfig.getParams().get(key, tClass);
    }

    private void initBaseInfo(Node parent, String indexInParent, NodeConfig nodeConfig, CombineConfig combineConfig) {
        this.parent = parent;
        this.nodeConfig = nodeConfig;
        this.combineConfig = combineConfig;
        //将公用配置读取到本节点配置中
        if (combineConfig != null) {
            combineConfig.params.forEach(nodeConfig::buildParam);
            this.typeName = combineConfig.typeName;
        }
        if (indexInParent == null) {
            if (parent == null) {
                //如果没有父节点，说明该节点就是父节点，加个father后缀
                indexInParent = String.format("%s_%s", getTypeName(), FatherNode.ID);
            } else {
                //如果有父节点，取父节点该类型子节点列表大小值+1（这个值会递增）
                int index = parent.typeToChildNodes.computeIfAbsent(getTypeName(), s -> new ArrayList<>()).size() + 1;
                indexInParent = String.format("%s_%s", getTypeName(), index);
            }
        }
        indexId = indexInParent;
        if (parent != null) {//处理子节点
            parent.addChildNode(this);
        } else {//处理父节点
            address = indexId;
            return;
        }
        /*
         * 读取分组信息
         */
        if (nodeConfig.activityId != null) {
            activityId = nodeConfig.activityId;
        } else {
            activityId = parent.activityId;
        }
        /*
         * 设置tag信息
         */
        if (nodeConfig.tag != null) {
            tag = nodeConfig.tag;
        }
        path = parent.address;
        address = path + "/" + indexId;
    }


    public void addChildNode(Node childNode) {
        childMap.put(childNode.getIndexId(), childNode);
        typeToChildNodes.computeIfAbsent(childNode.getTypeName(), s -> new ArrayList<>()).add(childNode);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("node toString ->");
        Collection<EventPoint> pointsAll = eventPoints.values();
        pointsAll.forEach(item -> builder.append("[").append(item.eventId).append(":").append(item.eventType).append("]"));
        return builder.toString();
    }
}
