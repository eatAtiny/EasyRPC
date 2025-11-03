package com.easy.simple.rpc.serializer;

import lombok.Getter;

/**
 * 序列化器类型枚举
 */
public enum SerializerType {
    
    /**
     * JDK序列化器
     */
    JDK("jdk", 0),
    /**
     * JSON序列化器
     */
    JSON("json", 1),
    /**
     * Hessian序列化器
     */
    HESSIAN("hessian", 2),

    /**
     * Kryo序列化器
     */
    KRYO("kryo", 3),

    /**
     * Protobuf序列化器
     */
    PROTOBUF("protobuf", 4);

    /**
     * -- GETTER --
     *  获取类型字符串
     *
     * @return 序列化器类型字符串
     */
    @Getter
    private final String type;
    @Getter
    private final int key;

    /**
     * 构造函数
     *
     * @param type 序列化器类型字符串
     * @param key 序列化器类型键值
     */
    SerializerType(String type, int key) {
        this.type = type;
        this.key = key;
    }

    /**
     * 根据key获取枚举
     *
     * @param key 序列化器类型键值
     * @return 序列化器类型枚举
     */
    public static SerializerType getEnumByKey(int key) {
        for (SerializerType anEnum : SerializerType.values()) {
            if (anEnum.key == key) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 根据key获取type字符串
     *
     * @param key 序列化器类型键值
     * @return 序列化器类型字符串
     */
    public static String getTypeByKey(int key) {
        SerializerType serializerType = getEnumByKey(key);
        return serializerType != null ? serializerType.getType() : null;
    }

    /**
     * 根据type字符串获取key
     *
     * @param type 序列化器类型字符串
     * @return 序列化器类型键值
     */
    public static int getKeyByType(String type) {
        for (SerializerType anEnum : SerializerType.values()) {
            if (anEnum.type.equals(type)) {
                return anEnum.key;
            }
        }
        return -1;
    }
}