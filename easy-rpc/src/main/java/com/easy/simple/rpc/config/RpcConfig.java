package com.easy.simple.rpc.config;

import com.easy.simple.rpc.serializer.SerializerType;
import com.easy.simple.rpc.server.HttpServer;
import com.easy.simple.rpc.server.VertxHttpServer;
import lombok.Data;

/**
 * RPC框架配置类
 * 使用单例模式管理全局配置
 */
@Data
public class RpcConfig {
    
    // 默认服务端口
    private int serverPort = 8080;
    
    // 默认服务器类型
    private String serverType = "vertx";
    
    // 远程服务ip
    private String serviceHost = "http://localhost";

    // mock 服务
    private boolean mock = false;

    // 序列化器
    private String serializerType = SerializerType.JDK.getType();

    // 注册中心配置
    private RegistryConfig registryConfig = new RegistryConfig();

    // 服务器
    private HttpServer server = new VertxHttpServer();
}