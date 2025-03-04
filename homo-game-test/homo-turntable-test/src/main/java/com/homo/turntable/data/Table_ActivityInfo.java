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
public class Table_ActivityInfo  {
	private final static Logger logger = LoggerFactory.getLogger(Table_ActivityInfo.class);
	public static String META_INFO = "__META_INFO__"; 

	public static int ROW_OFFSET = 4;

	public Table_ActivityInfo() {
	}

	@Data
	public static class Record_ActivityInfo{
		public int __ROW_INDEX__;
			/**
			* 下标
			*/
						public final Integer index;
			/**
			* 活动id
			*/
						public final String actId;
			/**
			* 活动类型
			*/
						public final Integer actType;
			/**
			* 活动子类型
			*/
						public final Integer actSubType;
			/**
			* 是否热点活动
			*/
						public final Integer actHot;
			/**
			* 活动名称
			*/
						public final String actName_i2;
			/**
			* 活动名称
			*/
						public final String actName;
			/**
			* 活动描述
			*/
						public final String actDesc_i2;
			/**
			* 活动描述
			*/
						public final String actDesc;
			/**
			* 优先级
			*/
						public final Integer sortId;
			/**
			* 开启条件标记
			*/
						public final String openConditionMark;
			/**
			* 角标
			*/
						public final Integer cornerMarkType;
			/**
			* 开启类型
			*/
						public final Integer joinType;
			/**
			* 开启时间
			*/
						public final String startTime;
			/**
			* 结算时间
			*/
						public final String endTime;
			/**
			* 关闭时间
			*/
						public final String closeTime;
			/**
			* 在该时间后注册才开启
			*/
						public final Long registerTimeAfterOpen;
			/**
			* 在该时间前注册才开启
			*/
						public final Long registerTimeBeforeOpen;
			/**
			* 展示条件类型
			*/
						public final Integer showConditionType;
			/**
			* 展示条件值
			*/
						public final Integer showConditionValue;
			/**
			* 支持渠道
			*/
						public final String channel;
			/**
			* 活动额外参数
			*/
						public final String extParams;
			/**
			* 总开关
			*/
						public final Integer enable;
			/**
			* lua脚本路径
			*/
						public final String luaPath;
			/**
			* 预制资源路径
			*/
						public final String resPath;
			/**
			* lua脚本名称
			*/
						public final String luaName;
			/**
			* 子预制体资源路径
			*/
						public final String subResPath;
			/**
			* 是否全屏
			*/
						public final Integer fullscreen;
		public String getKey__tpf() {
				String __key__ = index + "";
			return __key__;
		}
	}

	final String fileName = "ActivityInfo";
	String finalJsonPath = "ActivityInfo.json";

	public List<Record_ActivityInfo> getConfigs() {
		return configsAndKeyToIndexPair.getKey();
	}

	Pair<List<Record_ActivityInfo>, Map<String, Integer>> configsAndKeyToIndexPair = new Pair<>(new ArrayList<Record_ActivityInfo>(), new HashMap<String, Integer>());

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
            Record_ActivityInfo record = JSON.parseObject(value.toJSONString(), Record_ActivityInfo.class, disableDecimalFeature);
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
	public Record_ActivityInfo get(int i){
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
	 * @param index
	 * @return
	 */
	public Record_ActivityInfo getRecordByKey(Integer index){
            String __key__ = index + "";
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
	public Record_ActivityInfo getRecordByKey(String key){
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
		List<Record_ActivityInfo> tmpConfigs = new ArrayList<>(configsAndKeyToIndexPair.getKey());
		Map<String, Integer> tmpKeyToIndex = new HashMap<>(configsAndKeyToIndexPair.getValue());
		tmpConfigs.sort((o1, o2) -> (int)(o1.index - o2.index));
		// 重新计算索引
		tmpKeyToIndex = calcMapIndex(tmpConfigs);
		Pair<List<Record_ActivityInfo>, Map<String, Integer>> tmpPair = new Pair<>(tmpConfigs, tmpKeyToIndex);
		configsAndKeyToIndexPair = tmpPair;
	}

	private Map<String, Integer> calcMapIndex(List<Record_ActivityInfo> tmpConfigs) {
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
		List<Record_ActivityInfo> tmpConfigs = new ArrayList<>(configsAndKeyToIndexPair.getKey());
		Map<String, Integer> tmpKeyToIndex = new HashMap<>(configsAndKeyToIndexPair.getValue());
		tmpConfigs.sort((o1, o2) -> (int)(o2.index - o1.index));
		// 重新计算索引
		tmpKeyToIndex = calcMapIndex(tmpConfigs);
		Pair<List<Record_ActivityInfo>, Map<String, Integer>> tmpPair = new Pair<>(tmpConfigs, tmpKeyToIndex);
		configsAndKeyToIndexPair = tmpPair;
	}

	/**
	 * 获得key值最大的对象
	 * @return
	 */
	public Record_ActivityInfo getMaxKey(){
        return configsAndKeyToIndexPair.getKey().stream().max(Comparator.comparingInt(Record_ActivityInfo::getIndex)).orElse(null);
	}

	/**
	 * 获得key最小的对象
	 * @return
	 */
	public Record_ActivityInfo getMinKey(){
        return configsAndKeyToIndexPair.getKey().stream().min(Comparator.comparingInt(Record_ActivityInfo::getIndex)).orElse(null);
	}
	/**
	 * 策略查找
	 * @param predicate
	 * @return
	 */
	public List<Record_ActivityInfo> findByFilter(Predicate<? super Record_ActivityInfo> predicate){
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