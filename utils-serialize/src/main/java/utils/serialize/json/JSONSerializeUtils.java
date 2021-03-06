package utils.serialize.json;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

import utils.PrimitiveUtils;

/**
 * SerializeUtils 提供统一的序列化反序列化操作实现；
 * 
 * @author haiq
 *
 */
public abstract class JSONSerializeUtils {

	/**
	 * 启用自动配置服务；
	 * <p>
	 * 
	 * 自动配置服务通过 {@link JSONAutoConfigure} 定义，实现者以 SPI 方式提供；<br>
	 * 
	 * 调用此方法将触发一次对所有的 {@link JSONAutoConfigure} 提供者的加载；
	 * 
	 * 
	 */
	public synchronized static void enableAutoConfigure() {
		JSONGlobalConfigurator.initConfiguration();
	}
	
	public static SerializeConfig getSerializeConfig() {
		return JSONGlobalConfigurator.SERIALIZE_CONFIG;
	}

	public static ParserConfig getParserConfig() {
		return JSONGlobalConfigurator.PARSER_CONFIG;
	}

	public static void configProxyInterfaces(Class<?>... types) {
		JSONGlobalConfigurator.INSTANCE.configProxyInterfaces(types);
	}
	
	public static void registerDynamicTypeConverter(DynamicTypeConverter typeConverter) {
		JSONGlobalConfigurator.INSTANCE.registerDynamicTypeConverter(typeConverter);
	}

	public static void configDeserializeTypeMapping(Class<?> fromClazz, Class<?> toClazz) {
		JSONGlobalConfigurator.INSTANCE.configDeserializeTypeMapping(fromClazz, toClazz);
	}

	public static void configSerialization(Class<?> clazz, ObjectSerializer serializer,
			ObjectDeserializer deserializer) {
		JSONGlobalConfigurator.INSTANCE.configSerialization(clazz, serializer, deserializer);
	}

//	public static void configOutputTypeName(Class<?> clazz, boolean enable) {
//		JSONGlobalConfigurator.INSTANCE.configOutputTypeName(clazz, enable);
//	}

	public static void configSerialization(Class<?> clazz, ObjectSerializer serializer) {
		JSONGlobalConfigurator.INSTANCE.configSerializer(clazz, serializer);
	}

	public static void configDeserializer(Class<?> clazz, ObjectDeserializer deserializer) {
		JSONGlobalConfigurator.INSTANCE.configDeserializer(clazz, deserializer);
	}

	/**
	 * 配置指定的类型在序列化时总是输出 {@link Object#toString()} 方法的结果 ;
	 * 
	 * @param type
	 */
	public static void configStringSerializer(Class<?> type) {
		JSONGlobalConfigurator.INSTANCE.configStringSerializer(type);
	}

	/**
	 * 禁用循环引用检测；
	 * 
	 * <br>
	 * 默认是开启的；
	 * 
	 * @param type
	 */
	public static void disableCircularReferenceDetect() {
		JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.getMask();
	}

	private JSONSerializeUtils() {
	}

	public static Type getGenericType(Object obj) {
		return getGenericTypes(obj)[0];
	}

	public static Type[] getGenericTypes(Object obj) {
		Type superClass = obj.getClass().getGenericSuperclass();

		Type[] types = ((ParameterizedType) superClass).getActualTypeArguments();
		return types;
	}

	/**
	 * 判断是否是 JSON 字符；
	 * <p>
	 * 
	 * 此方法判断的JSON 字符的方法是检查指定的字符是否由花括号 { } 或者方括号 [ ] 或者双引号 ""包围；
	 * <p>
	 * 
	 * 这只是一种简化但不严谨的方法，检查通过返回 true 也不代表在花括号 { } 或者方括号 [ ] 或者双引号 "" 之间的内容符合 JSON 语法；
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isJSON(String str) {
		return isJSONObject(str) || isJSONOArray(str) || isJSONOValue(str);
	}

	/**
	 * 判断是否是 JSON 对象；
	 * <p>
	 * 
	 * 此方法判断的JSON 对象的方法是检查指定的字符是否由花括号 { } 包围；
	 * <p>
	 * 
	 * 这只是一种简化但不严谨的方法，检查通过返回 true 也不代表在花括号 { } 之间的内容符合 JSON 语法；
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isJSONObject(String str) {
		return str.startsWith("{") && str.endsWith("}");
	}

	/**
	 * 判断是否是 JSON 数组；
	 * <p>
	 * 
	 * 此方法判断的JSON 数组的方法是检查指定的字符是否由方括号 [ ] 包围；
	 * <p>
	 * 
	 * 这只是一种简化但不严谨的方法，检查通过返回 true 也不代表在方括号 [ ] 之间的内容符合 JSON 语法；
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isJSONOArray(String str) {
		return str.startsWith("[") && str.endsWith("]");
	}

	/**
	 * 判断是否是 JSON 值；
	 * <p>
	 * 
	 * 此方法判断的JSON 字符的方法是检查指定的字符是否由双引号 "" 包围；
	 * <p>
	 * 
	 * 这只是一种简化但不严谨的方法，检查通过返回 true 也不代表在双引号 "" 之间的内容符合 JSON 语法；
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isJSONOValue(String str) {
		return str.startsWith("\"") && str.endsWith("\"");
	}

	/**
	 * 将对象序列化为 JSON 字符串；（紧凑格式）
	 * 
	 * @param data
	 * @return
	 */
	public static String serializeToJSON(Object data) {
		// if (data instanceof JSONObject) {
		// return ((JSONObject) data).toJSONString();
		// }
		return serializeToJSON(data, null, false);
	}

	/**
	 * 将对象序列化为 JSON 字符串；（紧凑格式）
	 * 
	 * @param data
	 * @return
	 */
	public static String serializeToJSON(Object data, Class<?> serializedType) {
		return serializeToJSON(data, serializedType, false);
	}

	/**
	 * 将对象序列化为 JSON 字符串；
	 * 
	 * @param data         要序列化的对象；
	 * @param prettyFormat 是否以包含换行和缩进的良好格式输出 JSON;
	 * @return
	 */
	public static String serializeToJSON(Object data, boolean prettyFormat) {
		return serializeToJSON(data, null, prettyFormat);
	}

	/**
	 * 将对象序列化为 JSON 字符串；
	 * 
	 * @param data           要序列化的对象；
	 * @param serializedType 要序列化的对象的输出的类型；<br>
	 *                       指定该对象的父类或者某一个实现的接口类型，序列化输出的 JSON 将只包含该类型的属性；<br>
	 *                       如果指定为 null, 则按对象本身的类型进行序列化；
	 * @param prettyFormat   是否以包含换行和缩进的良好格式输出 JSON;
	 * @return
	 */
	public static String serializeToJSON(Object data, Class<?> serializedType, boolean prettyFormat) {
		return serializeToJSON(data, serializedType, null, prettyFormat);
	}

	/**
	 * 将对象序列化为 JSON 字符串；
	 * 
	 * @param data           要序列化的对象；
	 * @param serializedType 要序列化的对象的输出的类型；<br>
	 *                       指定该对象的父类或者某一个实现的接口类型，序列化输出的 JSON 将只包含该类型的属性；<br>
	 *                       如果指定为 null, 则按对象本身的类型进行序列化；
	 * @param dateFormat     日期格式；
	 * @param prettyFormat   是否以包含换行和缩进的良好格式输出 JSON;
	 * @return
	 */
	public static String serializeToJSON(Object data, Class<?> serializedType, String dateFormat,
			boolean prettyFormat) {
		SerializeWriter out;

		if (prettyFormat) {
			out = new SerializeWriter((Writer) null, JSON.DEFAULT_GENERATE_FEATURE, SerializerFeature.PrettyFormat);
		} else {
			out = new SerializeWriter((Writer) null, JSON.DEFAULT_GENERATE_FEATURE, SerializerFeature.EMPTY);
		}

		try {
			if (data == null) {
				return null;
				// out.writeNull();
			} else {
				// 确定要序列化的类型；
				if (serializedType == null) {
					serializedType = data.getClass();
				} else if ((!PrimitiveUtils.isPrimitiveType(serializedType))
						&& (!PrimitiveUtils.isPrimitiveType(data.getClass()))
						&& (!serializedType.isAssignableFrom(data.getClass()))) {
					throw new IllegalArgumentException("The serialized type[" + serializedType.getName()
							+ "] isn't assignable from the data type[" + data.getClass().getName() + "]!");
				}

				if (PrimitiveUtils.isWrapping(data.getClass(), serializedType)) {
					// 避免 serializedType 原生的值类型时引发 fastjson 的序列化异常；
					serializedType = data.getClass();
				}

				JSONSerializer serializer = new JSONSerializer(out, JSONGlobalConfigurator.SERIALIZE_CONFIG);

				// 配置日期格式；
				if (dateFormat != null && dateFormat.length() != 0) {
					serializer.setDateFormat(dateFormat);
					serializer.config(SerializerFeature.WriteDateUseDateFormat, true);
				}

				// 序列化；
				ObjectSerializer writer = serializer.getObjectWriter(serializedType);

				writer.write(serializer, data, null, null, JSON.DEFAULT_GENERATE_FEATURE);
			}
			return out.toString();
		} catch (IOException e) {
			throw new IllegalStateException(
					"Error occurred on serializing type[" + serializedType.getName() + "]! --" + e.getMessage(), e);
		} finally {
			out.close();
		}
	}

	/**
	 * 
	 * @param json
	 * @param dataClazz
	 * @return
	 */
	public static <T> T deserializeFromJSON(String json, Class<T> dataClazz) {
		return JSON.parseObject(json, dataClazz, JSONGlobalConfigurator.PARSER_CONFIG);
	}

	/**
	 * 将指定的 JSONObject 反序列化为指定的数据接口类型；
	 * 
	 * @param jsonObj       JSON 数据；
	 * @param dataInterface 对象接口类型；
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T deserializeFromJSONObject(JSONObject jsonObj, Class<T> dataInterface) {
		return (T) Proxy.newProxyInstance(dataInterface.getClassLoader(), new Class[] { dataInterface }, jsonObj);
	}

	/**
	 * 
	 * @param json
	 * @param dataClazz
	 * @return
	 */
	public static <T> T deserializeFromJSON(String json, GenericType<T> type) {
		return JSON.parseObject(json, type.getTypeArgument(), JSONGlobalConfigurator.PARSER_CONFIG);
	}

	@SuppressWarnings("unchecked")
	public static <T> T deserializeAs(Object data, Class<T> clazz) {
		if (data == null) {
			return null;
		}
		if (data instanceof JSON) {
			return ((JSON) data).toJavaObject(clazz);
		}
		if (data instanceof JSONBean) {
			return ((JSONBean) data).toJavaBean(clazz);
		}
		if (data instanceof String) {
			if (clazz.isInterface()) {
				JSONObject jsonObj = JSONSerializeUtils.deserializeAs(data, JSONObject.class);
				return deserializeFromJSONObject(jsonObj, clazz);
			}
			if (isJSON((String) data)) {
				return deserializeFromJSON((String) data, clazz);
			}
			if (clazz.isEnum()) {
				return deserializeFromJSON("\"" + data + "\"", clazz);
			}
		}
		if (data instanceof JSONString) {
			String jsonStr = ((JSONString) data).toString();
			if (isJSON(jsonStr)) {
				return deserializeFromJSON(jsonStr, clazz);
			} else {
				data = jsonStr;
			}
		}
		if (PrimitiveUtils.isPrimitiveType(clazz)) {
			return PrimitiveUtils.castTo(data, clazz);
		}
		if (clazz.isAssignableFrom(data.getClass())) {
			return (T) data;
		}
		if (clazz.isAssignableFrom(String.class)) {
			return (T) data.toString();
		}
		throw new IllegalArgumentException("Unsupported deserialization from type[" + data.getClass().toString()
				+ "] to type[" + clazz.toString() + "]!");
	}

}
