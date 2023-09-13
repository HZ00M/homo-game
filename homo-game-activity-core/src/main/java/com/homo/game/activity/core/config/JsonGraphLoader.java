package com.homo.game.activity.core.config;

import com.alibaba.fastjson.JSONObject;
import com.homo.game.activity.core.Node;
import com.homo.game.activity.core.node.InSideCarNode;
import com.homo.game.activity.core.node.OutSideCarNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import reactor.util.function.Tuples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.homo.game.activity.core.config.CombineConfig.Connect;

/**
 * json组合节点加载器
 * json配置数据由节点编辑器生成
 * 程序启动时会加载json配置数据
 */
@Slf4j
public class JsonGraphLoader {
    static final String KEY_FATHER_TYPE = "baseType";
    static final String KEY_TYPE_NAME = "nodeType";
    static final String KEY_TAG = "tag";
    static final String KEY_ACTIVITY_ID = "activityId";
    static final String KEY_NODE_TYPE_INDEX = "nodeTypeIndex";
    static final String KEY_ASK = "Ask";
    static final String KEY_PUBLISH = "publish";
    static final String KEY_SUBSCRIPTION = "subscription";
    static final String KEY_REPLY = "Reply";
    static final String KEY_PARAMS = "Params";
    static final String KEY_CHILD_NODE = "childNode";
    static final String KEY_GRAPH = "graph";
    static final String KEY_ASK_TO = "askTo";
    static final String KEY_PUBLISH_TO = "publishTo";

    public static String readFile(String path) {
        BufferedReader reader = null;
        String laststr = "";
        try {
            InputStream inputStream = JsonGraphLoader.class.getClassLoader().getResourceAsStream(path);
            //设置字符编码为UTF-8，避免读取中文乱码
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            // 通过BufferedReader进行读取
            reader = new BufferedReader(inputStreamReader);
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                laststr = laststr + tempString;
            }
            //关闭BufferedReader
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    //不管执行是否出现异常，必须确保关闭BufferedReader
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return laststr;
    }

    public static CombineConfig parseCombineConfig(String typeName, JSONObject jsonObject) {
        String baseType = jsonObject.getString(KEY_FATHER_TYPE);
        CombineConfig combineConfig = new CombineConfig(typeName, baseType);
        combineConfig.typeId = jsonObject.getInteger(KEY_NODE_TYPE_INDEX);
        combineConfig.activityId = jsonObject.getString(KEY_ACTIVITY_ID);
        process(jsonObject, KEY_ASK, askJson -> {
            combineConfig.ask = new ArrayList<>(askJson.getInnerMap().keySet());
        });
        process(jsonObject, KEY_REPLY, replyJson -> {
            combineConfig.reply = new ArrayList<>(replyJson.getInnerMap().keySet());
        });
        process(jsonObject, KEY_PUBLISH, pubJson -> {
            combineConfig.publish = new ArrayList<>(pubJson.getInnerMap().keySet());
        });
        process(jsonObject, KEY_SUBSCRIPTION, subJson -> {
            combineConfig.subscription = new ArrayList<>(subJson.getInnerMap().keySet());
        });
        process(jsonObject, KEY_CHILD_NODE, childArray -> {
            combineConfig.childNodeConfigs = childArray.getInnerMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, childJson -> parseNodeConfig(childJson.getKey(), (JSONObject) childJson.getValue())));
        });
        combineConfig.childNodeConfigs.forEach(((nodeId, nodeConfig) ->
                nodeConfig.params.data.getInnerMap().forEach(((paramName, value) ->
                        log.info("typeName {} childNode {} combineConfig ({}:{})", typeName, nodeId, paramName, value)))));
        process(jsonObject, KEY_GRAPH, graphJson -> {
            combineConfig.connects = graphJson.getInnerMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, connectJson -> parseNodeConnect((JSONObject) connectJson)));
        });

        combineConfig.connects.forEach(((srcNodeId, connect) -> {
            Assert.isTrue(combineConfig.childNodeConfigs.containsKey(srcNodeId) || InSideCarNode.ID.equals(srcNodeId) || OutSideCarNode.ID.equals(srcNodeId),
                    srcNodeId);
            connect.askTo.forEach(((askPointName, stringStringTpfTuple2) -> {
                log.info("typeName {} childNode {} askPointName {} askTo childNode {} replyPointName {}", typeName, srcNodeId, askPointName, stringStringTpfTuple2.getT1(), stringStringTpfTuple2.getT2());
                Assert.isTrue(combineConfig.childNodeConfigs.containsKey(stringStringTpfTuple2.getT1()) || InSideCarNode.ID.equals(stringStringTpfTuple2.getT1()) || Node.FATHER_ID.equals(stringStringTpfTuple2.getT1()));
            }));
            connect.pubTo.forEach(((subscriptionPointName, nodeIdToPointNames) -> nodeIdToPointNames.forEach((nodeId, publishPointName) -> {
                log.info("typeName {} childNode {} subName {} publishTo childNode {} pubName {}", typeName, srcNodeId, subscriptionPointName, nodeId, publishPointName);
                Assert.isTrue(combineConfig.childNodeConfigs.containsKey(nodeId) || OutSideCarNode.ID.equals(nodeId) || Node.FATHER_ID.equals(nodeId), nodeId);
            })));
        }));
        return combineConfig;
    }

    public static NodeConfig parseNodeConfig(String indexId, JSONObject jsonObject) {
        NodeConfig nodeConfig = new NodeConfig(indexId, jsonObject.getString(KEY_TYPE_NAME));
        nodeConfig.setTag(jsonObject.getString(KEY_TAG));
        process(jsonObject, KEY_PARAMS, paramsJson -> nodeConfig.params.data = paramsJson);
        process(jsonObject, KEY_ASK, askJson -> {
            nodeConfig.ask = new ArrayList<>(askJson.getInnerMap().keySet());
        });
        process(jsonObject, KEY_REPLY, replyJson -> {
            nodeConfig.reply = new ArrayList<>(replyJson.getInnerMap().keySet());
        });
        process(jsonObject, KEY_PUBLISH, pubJson -> {
            nodeConfig.publish = new ArrayList<>(pubJson.getInnerMap().keySet());
        });
        process(jsonObject, KEY_SUBSCRIPTION, subJson -> {
            nodeConfig.subscription = new ArrayList<>(subJson.getInnerMap().keySet());
        });
        return nodeConfig;
    }

    public static Connect parseNodeConnect(JSONObject jsonObject) {
        Connect connect = new Connect();
        JSONObject askToJson = jsonObject.getJSONObject(KEY_ASK_TO);
        if (askToJson != null) {
            connect.askTo = askToJson.getInnerMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, ackJson -> {
                        /**
                         * key -> askPoint
                         * value -> map(indexId-replyPoint)
                         */
                        Map.Entry<String, Object> entry = (Map.Entry<String, Object>) ((JSONObject) ackJson.getValue()).getInnerMap().entrySet().toArray()[0];
                        String configId = entry.getKey();
                        String subPoint = (String) entry.getValue();
                        return Tuples.of(configId, subPoint);
                    }));
        }
        JSONObject pubToJson = jsonObject.getJSONObject(KEY_PUBLISH_TO);
        if (pubToJson != null) {
            /**
             * (pubPoint-map(indexId-subPoint))
             */
            connect.pubTo = pubToJson.getInnerMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, pubJson ->
                            ((JSONObject) pubJson.getValue()).getInnerMap().entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, point -> (String) point.getValue()))
                    ));
        }
        return connect;
    }

    // 工具函数，方便处理属性不存在的情况
    public static void process(JSONObject jsonObject, String key, Consumer<JSONObject> jsonObjectConsumer) {
        JSONObject keyObj = jsonObject.getJSONObject(key);
        if (keyObj != null) {
            jsonObjectConsumer.accept(keyObj);
        }
    }
}
