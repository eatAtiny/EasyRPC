package com.easy.simple.rpc.serializer;

import com.easy.simple.rpc.serializer.impl.JdkSerializer;
import com.easy.simple.rpc.serializer.impl.JsonSerializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化器工厂
 * 采用静态工厂方法模式，结合单例模式缓存序列化器实例
 */
public class SerializerFactory {
    
    /**
     * 缓存序列化器实例，避免重复创建
     */
    private static final Map<String, Serializer> SERIALIZER_CACHE = new ConcurrentHashMap<>(){
        {
            put(SerializerType.JDK.getType(), new JdkSerializer());
            put(SerializerType.JSON.getType(), new JsonSerializer());
        }
    };
    
    /**
     * 私有构造函数，防止外部实例化
     */
    private SerializerFactory() {
    }

    /**
     * 默认序列化器（JDK）
     */
     private static final Serializer DEFAULT_SERIALIZER = SERIALIZER_CACHE.get(SerializerType.JDK.getType());

    /**
     * 根据序列化器类型获取序列化器实例
     * @param serializerType 序列化器类型
     * @return 序列化器实例
     */
    public static Serializer getInstance(String serializerType) {
        // 从缓存获取序列化器实例
        return SERIALIZER_CACHE.getOrDefault(serializerType, DEFAULT_SERIALIZER);
    }

}