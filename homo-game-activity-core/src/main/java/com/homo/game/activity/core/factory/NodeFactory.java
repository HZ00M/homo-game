package com.homo.game.activity.core.factory;

import com.homo.core.utils.lang.KKMap;
import com.homo.game.activity.core.Node;
import com.homo.game.activity.core.config.CombineConfig;
import com.homo.game.activity.core.config.NodeConfig;
import com.homo.game.activity.core.Point;
import com.homo.game.activity.facade.annotation.NodeType;
import com.homo.game.activity.facade.component.Single;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点工厂实现类
 * 管理节点配置信息和实例信息
 */
@Log4j2
public class NodeFactory {

    /**
     * 根据address索引到node
     */
    static Map<String, Point> nodePool = new ConcurrentHashMap<>();

    /**
     * 扫描的文件后缀
     */
    private final static String RESOURCE_PATTERN = "/**/*.class";
    /**
     * typeName-Constructor
     * 节点构造函数列表
     */
    static Map<String, Constructor<? extends Node>> nodeConstructorMap = new HashMap<>();

    /**
     * typeId-combineTypeName
     * 节点类型索引对应节点名列表
     */
    static Map<Integer, String> idToCombineTypeMap = new HashMap<>();

    /**
     * 组合节点名对应组合节点配置列表
     * 这里的配置由json或表格生成
     */
    static Map<String, CombineConfig> combineConfigMap = new HashMap<>();

    /**
     * typeName-Single
     * 静态节点列表
     */
    static Map<String, Single> singleMap = new HashMap<>();

    /**
     * (activityId-tag-Point)
     * 根据activityId和tag来索引到节点
     */
    static KKMap<String, String, Point> activityTagToNodeMap = new KKMap<>();

    /**
     * 扫描packagePath 包下的所有类，初始化构造方法和对象
     *
     * @param packagePath
     */
    public static void init(String packagePath) {
        //spring工具类，可以获取指定路径下的全部类
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        //MetadataReader 的工厂类
        MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
        try {
            String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                    ClassUtils.convertClassNameToResourcePath(packagePath) + RESOURCE_PATTERN;
            Resource[] resources = resourcePatternResolver.getResources(pattern);
            for (Resource resource : resources) {
                //读取类信息
                String className = readerFactory.getMetadataReader(resource).getClassMetadata().getClassName();
                //将类加载进内存，会初始化
                Class<?> clazz = Class.forName(className);
                //不用处理接口或抽象类
                if (Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface()) {
                    continue;
                }
                if (Node.class.isAssignableFrom(clazz)) {
                    log.info("factory add node {}", clazz);
                    String typeName = Node.getTypeName(clazz);
                    Constructor<? extends Node> constructor = nodeConstructorMap.get(typeName);
                    if (constructor != null) {
                        log.error("factory repeat add old {} new {}", constructor.getDeclaringClass(), clazz);
                        System.exit(-1);
                    }
                    try {
                        Constructor<? extends Node> classConstructor = ((Class<? extends Node>) clazz).getConstructor();
                        nodeConstructorMap.put(typeName, classConstructor);
                        NodeType nodeType = clazz.getAnnotation(NodeType.class);
                        if (nodeType != null) {
                            idToCombineTypeMap.put(nodeType.value(), className);
                        }
                    } catch (NoSuchMethodException e) {
                        log.info("not default constructor {}", clazz);
                    }
                }
                //todo 扫描上报事件和请求事件类

            }
        } catch (Exception e) {
            log.error("DefaultNodeFactory init packagePath {} error!!", packagePath, e);
            System.exit(-1);
        }
    }

    /**
     * 通过地址获取一个节点实例
     */
    public static Point getPoint(String address) {
        return nodePool.get(address);
    }

    public static Point updatePoint(Point point){
        return nodePool.put(point.getAddress(),point);
    }

    /**
     * 添加一个组合节点配置
     */
    public static void addCombineConfig(CombineConfig combineConfig) {
        //检查是否重复添加
        Assert.isTrue(!idToCombineTypeMap.containsKey(combineConfig.typeId),
                String.format("addNodeType typeIndex conflict typeName %s typeId %s existTypeName %s",
                        combineConfig.typeName, combineConfig.typeId, idToCombineTypeMap.get(combineConfig.typeId)));
        Assert.isTrue(!combineConfigMap.containsKey(combineConfig.typeName),
                String.format("addNodeType typeIndex conflict typeName %s typeId %s existTypeName %s",
                        combineConfig.typeName, combineConfig.typeId, combineConfigMap.get(combineConfig.typeName)));
        /**
         * 类信息中必须包含fatherType指定的类型
         */
        if (!nodeConstructorMap.containsKey(combineConfig.fatherType)) {
            log.error("add combine typeName [{}] fatherType [{}] not found! ", combineConfig.typeName, combineConfig.fatherType);
        } else {
            log.info("add combine typeName [{}] fatherType [{}] typeId [{}]", combineConfig.typeName, combineConfig.fatherType, combineConfig.typeId);
        }

        /**
         * 建立类型索引（typeId）与类型名之间的关系
         */
        idToCombineTypeMap.put(combineConfig.typeId, combineConfig.typeName);

        /**
         * 检查组合节点中的子节点类型是否合法
         */
        combineConfig.childNodeConfigs.forEach(((s, nodeConfig) -> {
            if (!nodeConstructorMap.containsKey(nodeConfig.getTypeName()) && !idToCombineTypeMap.containsKey(nodeConfig.getTypeId())) {
                //该节点既不是基础节点也不是组合节点
                log.error("add combine [{}] typeName [{}] typeId [{}] index [{}] not found! ", combineConfig.typeName, nodeConfig.getTypeName(), nodeConfig.getTypeId(), nodeConfig.getIndexId());
            } else {
                log.info("add combine [{}] typeName [{}] typeId [{}] index [{}]", combineConfig.typeName, nodeConfig.getTypeName(), nodeConfig.getTypeId(), nodeConfig.getIndexId());
            }
        }));

        /**
         * 保存配置
         */
        combineConfigMap.put(combineConfig.typeName, combineConfig);
    }

    /**
     * 创建一个节点
     */
    public static Node newNode(NodeConfig nodeConfig) {
        return newNode(null, null, nodeConfig);
    }


    public static Node newNode(Node parent, String indexId, NodeConfig nodeConfig) {
        try {
            return createNode(parent, indexId, nodeConfig);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            log.error("node class not found ({}/{}/{}/{}) ", nodeConfig.getActivityId(), nodeConfig.getTag(), nodeConfig.getTypeName(), nodeConfig.getTypeId());
            log.error("请确保 node的实现有无参构造函数");
            System.exit(-1);
            return null;
        }
    }

    public static Node createNode(Node parent, String indexId, NodeConfig nodeConfig) throws InvocationTargetException, InstantiationException, IllegalAccessException, NoClassDefFoundError {
        String typeName = nodeConfig.getTypeName();
        Integer typeId = nodeConfig.getTypeId();
        if (typeId != null) {
            //说明是组合类型，找到组合类型的基础父类
            typeName = idToCombineTypeMap.get(typeId);
            if (typeName == null) {
                log.error("getOrCreateNode error typeId {} can't found!", typeId);
                return null;
            }
        }
        //获取该节点所在的组合节点的配置
        CombineConfig combineConfig = combineConfigMap.get(typeName);
        Constructor<? extends Node> nodeConstructor;
        if (combineConfig != null) {
            //说明需要创建的是组合节点
            nodeConstructor = nodeConstructorMap.get(combineConfig.fatherType);
        } else {
            nodeConstructor = nodeConstructorMap.get(typeName);
        }
        if (nodeConstructor == null) {
            throw new NoClassDefFoundError();
        }
        Node node = null;
        //根据无参构造函数创建节点
        node = nodeConstructor.newInstance();
        node.init(parent, indexId, nodeConfig, combineConfig);
        NodeFactory.updatePoint(node);
        if (node instanceof Single) {
            addSingle((Single) node);
        }
        return node;
    }

    public static void addSingle(Single singleNode) {
        String typeName = Node.getTypeName(singleNode.getClass());
        Assert.isTrue(!singleMap.containsKey(typeName), "repeat add single node " + typeName);
        singleMap.put(typeName, singleNode);
    }

    /**
     * 通过类获得一个静态节点组件
     * @param tClass
     * @param <T>
     * @return
     */
    public <T extends Single> T getSingle(Class<T> tClass){
        return (T) singleMap.get(Node.getTypeName(tClass));
    }
}
