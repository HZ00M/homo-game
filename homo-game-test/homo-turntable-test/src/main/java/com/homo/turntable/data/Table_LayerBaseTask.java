package com.homo.turntable.data;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.homo.core.utils.lang.Pair;
import lombok.Data;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class Table_LayerBaseTask  {
	private final static Logger logger = LoggerFactory.getLogger(Table_LayerBaseTask.class);
	public static String META_INFO = "__META_INFO__"; 

	public static int ROW_OFFSET = 4;

	public Table_LayerBaseTask() {
	}

	@Data
	public static class Record_LayerBaseTask{
		public int __ROW_INDEX__;
			/**
			* 下标
			*/
						public final Integer id;
			/**
			* 活动id
			*/
						public final String activityId;
			/**
			* 父任务id
			*/
						public final String parentTaskId;
			/**
			* 前置任务id
			*/
						public final String promiseTaskId;
			/**
			* 任务id
			*/
						public final String taskId;
			/**
			* 节点类型
			*/
						public final String nodeType;
			/**
			* 任务条件类型
			*/
						public final String taskType;
			/**
			* 达成值
			*/
						public final Integer targetProcess;
			/**
			* 任务权重
			*/
						public final Integer weight;
			/**
			* 条件
			*/
						public final String condition;
			/**
			* 奖励类型
			*/
						public final String rewardType;
			/**
			* 任务奖励
			*/
						public final String reward;
			/**
			* 任务描述
			*/
						public final String desc;
			/**
			* 任务排序
			*/
						public final Integer sortId;
			/**
			* 时间开启类型
			*/
						public final Integer timeType;
			/**
			* 开始时间
			*/
						public final Long startTime;
			/**
			* 结束时间
			*/
						public final Long endTime;
			/**
			* 开启时间
			*/
						public final String startTimeStr;
			/**
			* 结算时间
			*/
						public final String endTimeStr;
			/**
			* 偏移时间/秒
			*/
						public final Long offsetTime;
			/**
			* 额外信息
			*/
						public final String extraInfo;
			/**
			* 备注
			*/
						public final String mask;
		public String getKey__tpf() {
				String __key__ = id + "";
			return __key__;
		}
	}

	final String fileName = "LayerBaseTask";
	String finalJsonPath = "LayerBaseTask.json";

	public List<Record_LayerBaseTask> getConfigs() {
		return configsAndKeyToIndexPair.getKey();
	}

	Pair<List<Record_LayerBaseTask>, Map<String, Integer>> configsAndKeyToIndexPair = new Pair<>(new ArrayList<Record_LayerBaseTask>(), new HashMap<String, Integer>());

	/**
	* 加载数据
	*/
	public void load() {
        int disableDecimalFeature = JSON.DEFAULT_PARSER_FEATURE & ~Feature.UseBigDecimal.getMask();
		int index = 0;
		for(Map.Entry<String, Object> entry: getJsonObject().entrySet()){
			if (META_INFO.equals(entry.getKey())) {
				continue;
			}
			JSONObject value = (JSONObject)entry.getValue();
            Record_LayerBaseTask record = JSON.parseObject(value.toJSONString(), Record_LayerBaseTask.class, disableDecimalFeature);
			record.__ROW_INDEX__ = index + ROW_OFFSET;
			configsAndKeyToIndexPair.getKey().add(record);
			configsAndKeyToIndexPair.getValue().put(entry.getKey(), index);
			index ++;
		}
	}
	/**
	 * 获取第i个数据
	 * @param i
	 * @return
	 */
	public Record_LayerBaseTask get(int i){
		return configsAndKeyToIndexPair.getKey().get(i);
	}

	/**
	 * 获取数据的长度
	 * @return
	 */
	public int size(){
		return configsAndKeyToIndexPair.getKey().size();
	}

	/**
	 * 根据key值获得数据
	 * @param id
	 * @return
	 */
	public Record_LayerBaseTask getRecordByKey(Integer id){
            String __key__ = id + "";
			Integer __index__ = configsAndKeyToIndexPair.getValue().get(__key__);
			if (__index__ == null) {
				return null;
			}
            return configsAndKeyToIndexPair.getKey().get(__index__);
	}

	/**
	 * 根据key获得数据
	 * @param key
	 * @return
	 */
	public Record_LayerBaseTask getRecordByKey(String key){
		Integer index = configsAndKeyToIndexPair.getValue().get(key);
		if (index == null) {
			return null;
		}
		return configsAndKeyToIndexPair.getKey().get(index);
	}

	/**
	 * 根据key从小到大排序
	 */
	public void ascending(){
		List<Record_LayerBaseTask> tmpConfigs = new ArrayList<>(configsAndKeyToIndexPair.getKey());
		Map<String, Integer> tmpKeyToIndex = new HashMap<>(configsAndKeyToIndexPair.getValue());
		tmpConfigs.sort((o1, o2) -> (int)(o1.id - o2.id));
		// 重新计算索引
		tmpKeyToIndex = calcMapIndex(tmpConfigs);
		Pair<List<Record_LayerBaseTask>, Map<String, Integer>> tmpPair = new Pair<>(tmpConfigs, tmpKeyToIndex);
		configsAndKeyToIndexPair = tmpPair;
	}

	private Map<String, Integer> calcMapIndex(List<Record_LayerBaseTask> tmpConfigs) {
		Map<String, Integer> tmp = new HashMap<>();
		for (int i = 0; i < tmpConfigs.size(); i++) {
		tmp.put(tmpConfigs.get(i).getKey__tpf(), i);
		tmpConfigs.get(i).__ROW_INDEX__ = i + ROW_OFFSET;
		}
		return tmp;
		}

	/**
	 * 根据key从大到小排序
	 */
	public void descending(){
		List<Record_LayerBaseTask> tmpConfigs = new ArrayList<>(configsAndKeyToIndexPair.getKey());
		Map<String, Integer> tmpKeyToIndex = new HashMap<>(configsAndKeyToIndexPair.getValue());
		tmpConfigs.sort((o1, o2) -> (int)(o2.id - o1.id));
		// 重新计算索引
		tmpKeyToIndex = calcMapIndex(tmpConfigs);
		Pair<List<Record_LayerBaseTask>, Map<String, Integer>> tmpPair = new Pair<>(tmpConfigs, tmpKeyToIndex);
		configsAndKeyToIndexPair = tmpPair;
	}

	/**
	 * 获得key值最大的对象
	 * @return
	 */
	public Record_LayerBaseTask getMaxKey(){
        return configsAndKeyToIndexPair.getKey().stream().max(Comparator.comparingInt(Record_LayerBaseTask::getId)).orElse(null);
	}

	/**
	 * 获得key最小的对象
	 * @return
	 */
	public Record_LayerBaseTask getMinKey(){
        return configsAndKeyToIndexPair.getKey().stream().min(Comparator.comparingInt(Record_LayerBaseTask::getId)).orElse(null);
	}
	/**
	 * 策略查找
	 * @param predicate
	 * @return
	 */
	public List<Record_LayerBaseTask> findByFilter(Predicate<? super Record_LayerBaseTask> predicate){
		return configsAndKeyToIndexPair.getKey().stream().filter(predicate).collect(Collectors.toList());
	}


	private JSONObject getJsonObject(){
		try {
			String jsonPath = "/json/" + finalJsonPath;
			String jsonStr = readFile(jsonPath);
			return JSON.parseObject(jsonStr, Feature.OrderedField);
		}catch (Exception e) {
			logger.error("load config from configServer fail: {}", fileName, e);
		}
		return null;
	}
	private String readFile(String path) {
		BufferedReader reader = null;
		String laststr = "";
		try {
			InputStream inputStream = this.getClass().getResourceAsStream(path);
			//设置字符编码为UTF-8，避免读取中文乱码
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"UTF-8");
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
}