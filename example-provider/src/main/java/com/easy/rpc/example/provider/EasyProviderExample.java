package com.easy.rpc.example.provider;


import com.easy.example.common.service.UserService;
import com.easy.simple.rpc.RpcApplication;
import com.easy.simple.rpc.config.RpcConfig;
import com.easy.simple.rpc.enity.ServiceMetaInfo;
import com.easy.simple.rpc.registry.Registry;
import com.easy.simple.rpc.registry.RegistryFactory;
import com.easy.simple.rpc.registry.impl.LocalRegistry;
import com.easy.simple.rpc.server.HttpServer;

/**
 * 简易服务提供者示例
 */
public class EasyProviderExample {

    public static void main(String[] args) {
        // 配置 RPC 框架
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        System.out.println(rpcConfig);
        // 注册服务
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

        // 将服务注册到注册中心
        Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(UserService.class.getName());
        serviceMetaInfo.setServiceHost(rpcConfig.getServiceHost());
        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
        try{
            registry.register(serviceMetaInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 启动 web 服务
        HttpServer httpServer = rpcConfig.getServer();
        httpServer.doStart(rpcConfig.getServerPort());
    }
}
