package com.easy.simple.rpc.serializer;

/**
 * 序列化器类型枚举
 */
public enum SerializerType {
    
    /**
     * JDK序列化器
     */
    JDK("jdk"),
    
    /**
     * JSON序列化器
     */
    JSON("json");
    
    private final String type;
    
    /**
     * 构造函数
     * @param type 序列化器类型字符串
     */
    SerializerType(String type) {
        this.type = type;
    }
    
    /**
     * 获取类型字符串
     * @return 序列化器类型字符串
     */
    public String getType() {
        return type;
    }

}