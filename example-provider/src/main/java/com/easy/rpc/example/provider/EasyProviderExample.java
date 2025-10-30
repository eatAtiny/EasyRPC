package com.easy.rpc.example.provider;


import com.easy.example.common.service.UserService;
import com.easy.simple.rpc.registry.LocalRegistry;
import com.easy.simple.rpc.server.HttpServer;
import com.easy.simple.rpc.server.VertxHttpServer;

/**
 * 简易服务提供者示例
 */
public class EasyProviderExample {

    public static void main(String[] args) {
        // 注册服务
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

        // 启动 web 服务
        HttpServer httpServer = new VertxHttpServer();
        httpServer.doStart(8080);
    }
}
