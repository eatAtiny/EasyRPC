package com.easy.simple.rpc.config;

import com.easy.simple.rpc.serializer.Serializer;
import com.easy.simple.rpc.serializer.JdkSerializer;
import com.easy.simple.rpc.serializer.JSONSerializer;
import com.easy.simple.rpc.server.HttpServer;
import com.easy.simple.rpc.server.VertxHttpServer;
import lombok.Data;
import lombok.Getter;

/**
 * RPC框架配置类
 * 使用单例模式管理全局配置
 */
@Data
public class RpcConfig {
    
    // 默认服务端口
    private int serverPort = 8080;
    
    // 默认序列化器类型
    private String serializerType = "jdk";
    
    // 默认服务器类型
    private String serverType = "vertx";
    
    // 远程服务地址（客户端使用）
    private String serviceAddress = "http://localhost:8080";

    // 获取单例实例
    // 单例实例
    @Getter
    private static final RpcConfig instance = new RpcConfig();
    
    // 私有构造函数
    private RpcConfig() {
    }

    // 获取序列化器实例
    public Serializer getSerializer() {
        if ("json".equals(serializerType)) {
            return new JSONSerializer();
        }
        // 默认使用JDK序列化器
        return new JdkSerializer();
    }
    
    // 获取服务器实例
    public HttpServer getServer() {
        // 目前只有Vertx实现
        return new VertxHttpServer();
    }
}