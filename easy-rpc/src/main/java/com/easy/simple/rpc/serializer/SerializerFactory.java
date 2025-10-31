package com.easy.simple.rpc.serializer;

import com.easy.simple.rpc.serializer.impl.JdkSerializer;
import com.easy.simple.rpc.utils.SpiLoader;

/**
 * 序列化器工厂
 * 采用静态工厂方法模式，结合单例模式缓存序列化器实例
 */
public class SerializerFactory {

    /**
     * 懒加载标记，用于标识是否已经加载过序列化器
     */
    private static volatile boolean loaded = false;

    /**
     * 私有构造函数，防止外部实例化
     */
    private SerializerFactory() {
    }

    /**
     * 默认序列化器（JDK）
     */
    private static final Serializer DEFAULT_SERIALIZER = new JdkSerializer();

    /**
     * 根据序列化器类型获取序列化器实例（懒加载模式）
     * @param serializerType 序列化器类型
     * @return 序列化器实例
     */
    public static Serializer getInstance(String serializerType) {
        // 懒加载：只在第一次调用时加载序列化器
        if (!loaded) {
            synchronized (SerializerFactory.class) {
                if (!loaded) {
                    SpiLoader.load(Serializer.class);
                    loaded = true;
                }
            }
        }
        
        try {
            // 从SPI加载器获取序列化器实例
            return SpiLoader.getInstance(Serializer.class, serializerType);
        } catch (Exception e) {
            // 如果获取失败，返回默认序列化器
            return DEFAULT_SERIALIZER;
        }
    }
}