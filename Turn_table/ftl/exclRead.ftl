<#ftl encoding="utf-8">
package ${packagename};

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
public class Table_${className}  {
	private final static Logger logger = LoggerFactory.getLogger(Table_${className}.class);
	public static String META_INFO = "__META_INFO__"; 

	public static int ROW_OFFSET = 4;

	public Table_${className}() {
	}

	@Data
	public static class Record_${className}{
		public int __ROW_INDEX__;
		<#list fields as f>
			/**
			* ${f.common?trim}
			*/
			<#--枚举-->
			<#if isPackageType>
				<#if f.type?starts_with("e")>
					public final Table_ServerEnumConfig.${f.type?trim} ${f.name?trim};
				<#elseif f.type?starts_with("Map")>
					<#if f.type?keep_after("<")?keep_before(",")?starts_with("e")>public final Map<Table_ServerEnumConfig.${f.type?keep_after("<")?keep_before(",")} ,<#elseif f.type?keep_after("<")?keep_before(",")?contains("bool")>public final Map<Boolean ,<#elseif f.type?keep_after("<")?keep_before(",")?contains("int")>public final Map<Integer ,<#elseif f.type?keep_after("<")?keep_before(",")?contains("float")>public final Map<Float ,<#else>public final Map<${f.type?keep_after("<")?keep_before(",")?cap_first} ,</#if><#if f.type?keep_after(",")?keep_before(">")?starts_with("e")>Table_ServerEnumConfig.${f.type?keep_after("<")?keep_before(">")}> ${f.name?trim};<#elseif f.type?keep_after(",")?keep_before(">")?contains("bool")>Boolean> ${f.name?trim};<#elseif f.type?keep_after(",")?keep_before(">")?contains("int")>Integer> ${f.name?trim};<#elseif f.type?keep_after(",")?keep_before(">")?contains("float")>Float> ${f.name?trim};<#else>${f.type?keep_after(",")?keep_before(">")?cap_first}> ${f.name?trim};</#if>
				<#elseif f.type?starts_with("json")>
						public final JSONObject ${f.name?trim};
				<#elseif f.type?starts_with("List")>
					<#--list中枚举-->
					<#if f.type?keep_after("<")?keep_before(">")?starts_with("e")>
						public final List<Table_ServerEnumConfig.${f.type?keep_after("<")?keep_before(">")}> ${f.name?trim};
					<#--list中bool-->
					<#elseif f.type?keep_after("<")?keep_before(">")?contains("bool")>
						public final List<Boolean> ${f.name?trim};
					<#--list中Integer-->
					<#elseif f.type?keep_after("<")?keep_before(">")?contains("int")>
						public final List<Integer> ${f.name?trim};
					<#--list中Double-->
					<#elseif f.type?keep_after("<")?keep_before(">")?contains("float")>
						public final List<Float> ${f.name?trim};
					<#else>
					<#--list中其他-->
						public final List<${f.type?keep_after("<")?keep_before(">")?cap_first}> ${f.name?trim};
					</#if>
				<#else>
					<#--非枚举非list，是Integer啊-->
					<#if f.type?contains("int")>
						public final Integer ${f.name?trim};
					<#elseif f.type?contains("bool")>
						public final Boolean ${f.name?trim};
					<#--Double-->
					<#elseif f.type?contains("float")>
						public final Float ${f.name?trim};
					<#elseif f.type?contains("long")>
						public final Long ${f.name?trim};
					<#--非枚举非list非Integer，大写首字符-->
					<#else>
						public final ${f.type?trim?cap_first} ${f.name?trim};
					</#if>
				</#if>
			<#else>
				<#if f.type?starts_with("e")>
					public final Table_ServerEnumConfig.${f.type?trim} ${f.name?trim};
				<#elseif f.type?starts_with("Map")>
					<#if f.type?keep_after("<")?keep_before(",")?starts_with("e")>public final Map<Table_ServerEnumConfig.${f.type?keep_after("<")?keep_before(",")} ,<#elseif f.type?keep_after("<")?keep_before(",")?contains("bool")>public final Map<Boolean ,<#elseif f.type?keep_after("<")?keep_before(",")?contains("int")>public final Map<Integer ,<#elseif f.type?keep_after("<")?keep_before(",")?contains("float")>public final Map<Float ,<#else>public final Map<${f.type?keep_after("<")?keep_before(",")?cap_first} ,</#if><#if f.type?keep_after(",")?keep_before(">")?starts_with("e")>Table_ServerEnumConfig.${f.type?keep_after("<")?keep_before(">")}> ${f.name?trim};<#elseif f.type?keep_after(",")?keep_before(">")?contains("bool")>Boolean> ${f.name?trim};<#elseif f.type?keep_after(",")?keep_before(">")?contains("int")>Integer> ${f.name?trim};<#elseif f.type?keep_after(",")?keep_before(">")?contains("float")>Float> ${f.name?trim};<#else>${f.type?keep_after(",")?keep_before(">")?cap_first}> ${f.name?trim};</#if>
				<#elseif f.type?starts_with("json")>
						public final JSONObject ${f.name?trim};
				<#elseif f.type?starts_with("List")>
					<#--list中枚举-->
					<#if f.type?keep_after("<")?keep_before(">")?starts_with("e")>
						public final List<Table_ServerEnumConfig.${f.type?keep_after("<")?keep_before(">")}> ${f.name?trim};
					<#--list中bool-->
					<#elseif f.type?keep_after("<")?keep_before(">")?contains("bool")>
						public final List<Boolean> ${f.name?trim};
					<#--list中Integer-->
					<#elseif f.type?keep_after("<")?keep_before(">")?contains("int")>
						public final List<Integer> ${f.name?trim};
					<#--list中Double-->
					<#elseif f.type?keep_after("<")?keep_before(">")?contains("float")>
						public final List<Float> ${f.name?trim};
					<#else>
					<#--list中其他-->
						public final List<${f.type?keep_after("<")?keep_before(">")?cap_first}> ${f.name?trim};
					</#if>
				<#else>
					<#--非枚举非list，是Integer啊-->
					<#if f.type?contains("int")>
						public final int ${f.name?trim};
					<#elseif f.type?contains("bool")>
						public final boolean ${f.name?trim};
					<#--Double-->
					<#elseif f.type?contains("float")>
						public final float ${f.name?trim};
					<#elseif f.type?contains("long")>
						public final long ${f.name?trim};
					<#elseif f.type?contains("double")>
						public final double ${f.name?trim};
					<#elseif f.type?contains("string")>
						public final String ${f.name?trim};
					<#--非枚举非list非Integer，大写首字符-->
					<#else>
						public final ${f.type?trim} ${f.name?trim};
					</#if>
				</#if>
			</#if>
		</#list>
		public String getKey__tpf() {
			<#if keys?size == 1>
				String __key__ = <#list keys as k>${k.keyName} + ""</#list>;
			<#else>
				String __key__ = <#list keys as k>${k.keyName}<#sep> + "_" + </#sep></#list>;
			</#if>
			return __key__;
		}
	}

	final String fileName = "${filename}";
	String finalJsonPath = "${jsonPath?keep_after("\\json\\")?replace("\\", "/")}";

	public List<Record_${className}> getConfigs() {
		return configsAndKeyToIndexPair.getKey();
	}

	Pair<List<Record_${className}>, Map<String, Integer>> configsAndKeyToIndexPair = new Pair<>(new ArrayList<Record_${className}>(), new HashMap<String, Integer>());

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
            Record_${className} record = JSON.parseObject(value.toJSONString(), Record_${className}.class, disableDecimalFeature);
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
	public Record_${className} get(int i){
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
	 * @param ${keyname}
	 * @return
	 */
	public Record_${className} getRecordByKey(<#list keys as k><#if k.keyType?contains("int")>Integer<#elseif k.keyType?contains("bool")>Boolean<#elseif k.keyType?contains("float")>Float<#elseif k.keyType?starts_with("e")>Table_ServerEnumConfig.${k.keyType?trim}<#else>${k.keyType?trim?cap_first}</#if> ${k.keyName}<#sep>, </#sep></#list>){
            <#if keys?size == 1>
            String __key__ = <#list keys as k>${k.keyName} + ""</#list>;
            <#else>
            String __key__ = <#list keys as k>${k.keyName}<#sep> + "_" + </#sep></#list>;
            </#if>
			Integer __index__ = configsAndKeyToIndexPair.getValue().get(__key__);
			if (__index__ == null) {
				return null;
			}
            return configsAndKeyToIndexPair.getKey().get(__index__);
	}

	<#if keys?size != 1 || keyType?contains("int") || keyType?contains("long") || keyType?contains("float") || keyType?contains("double")>
	/**
	 * 根据key获得数据
	 * @param key
	 * @return
	 */
	public Record_${className} getRecordByKey(String key){
		Integer index = configsAndKeyToIndexPair.getValue().get(key);
		if (index == null) {
			return null;
		}
		return configsAndKeyToIndexPair.getKey().get(index);
	}
	</#if>

	<#if keyType?contains("int") || keyType?contains("long") || keyType?contains("float") || keyType?contains("double")>
	/**
	 * 根据key从小到大排序
	 */
	public void ascending(){
		List<Record_${className}> tmpConfigs = new ArrayList<>(configsAndKeyToIndexPair.getKey());
		Map<String, Integer> tmpKeyToIndex = new HashMap<>(configsAndKeyToIndexPair.getValue());
		tmpConfigs.sort((o1, o2) -> (int)(o1.${keyname} - o2.${keyname}));
		// 重新计算索引
		tmpKeyToIndex = calcMapIndex(tmpConfigs);
		Pair<List<Record_${className}>, Map<String, Integer>> tmpPair = new Pair<>(tmpConfigs, tmpKeyToIndex);
		configsAndKeyToIndexPair = tmpPair;
	}

	private Map<String, Integer> calcMapIndex(List<Record_${className}> tmpConfigs) {
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
		List<Record_${className}> tmpConfigs = new ArrayList<>(configsAndKeyToIndexPair.getKey());
		Map<String, Integer> tmpKeyToIndex = new HashMap<>(configsAndKeyToIndexPair.getValue());
		tmpConfigs.sort((o1, o2) -> (int)(o2.${keyname} - o1.${keyname}));
		// 重新计算索引
		tmpKeyToIndex = calcMapIndex(tmpConfigs);
		Pair<List<Record_${className}>, Map<String, Integer>> tmpPair = new Pair<>(tmpConfigs, tmpKeyToIndex);
		configsAndKeyToIndexPair = tmpPair;
	}

	/**
	 * 获得key值最大的对象
	 * @return
	 */
	public Record_${className} getMaxKey(){
    <#if keyType?contains("int")>
        return configsAndKeyToIndexPair.getKey().stream().max(Comparator.comparingInt(Record_${className}::get${keyname?cap_first})).orElse(null);
    <#elseif keyType?contains("long")>
        return configsAndKeyToIndexPair.getKey().stream().max(Comparator.comparingLong(Record_${className}::get${keyname?cap_first})).orElse(null);
    <#elseif keyType?contains("float")>
        return configsAndKeyToIndexPair.getKey().stream().max(Comparator.comparingDouble(Record_${className}::get${keyname?cap_first})).orElse(null);
	<#elseif keyType?contains("double")>
		return configsAndKeyToIndexPair.getKey().stream().max(Comparator.comparingDouble(Record_${className}::get${keyname?cap_first})).orElse(null);
    </#if>
	}

	/**
	 * 获得key最小的对象
	 * @return
	 */
	public Record_${className} getMinKey(){
    <#if keyType?contains("int")>
        return configsAndKeyToIndexPair.getKey().stream().min(Comparator.comparingInt(Record_${className}::get${keyname?cap_first})).orElse(null);
    <#elseif keyType?contains("long")>
        return configsAndKeyToIndexPair.getKey().stream().min(Comparator.comparingLong(Record_${className}::get${keyname?cap_first})).orElse(null);
    <#elseif keyType?contains("float")>
        return configsAndKeyToIndexPair.getKey().stream().min(Comparator.comparingDouble(Record_${className}::get${keyname?cap_first})).orElse(null);
	<#elseif keyType?contains("double")>
		return configsAndKeyToIndexPair.getKey().stream().min(Comparator.comparingDouble(Record_${className}::get${keyname?cap_first})).orElse(null);
    </#if>
	}
    </#if>
	/**
	 * 策略查找
	 * @param predicate
	 * @return
	 */
	public List<Record_${className}> findByFilter(Predicate<? super Record_${className}> predicate){
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