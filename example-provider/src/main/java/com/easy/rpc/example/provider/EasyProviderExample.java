package com.easy.rpc.example.provider;


import com.easy.example.common.service.UserService;
import com.easy.simple.rpc.config.RpcConfig;
import com.easy.simple.rpc.registry.LocalRegistry;
import com.easy.simple.rpc.server.HttpServer;
import com.easy.simple.rpc.server.VertxHttpServer;

/**
 * 简易服务提供者示例
 */
public class EasyProviderExample {

    public static void main(String[] args) {
        // 配置 RPC 框架
        RpcConfig rpcConfig = RpcConfig.getInstance();
        // 使用json序列化器
        rpcConfig.setSerializerType("json");

        // 注册服务
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

        // 启动 web 服务
        HttpServer httpServer = rpcConfig.getServer();
        httpServer.doStart(8080);
    }
}
