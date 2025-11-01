package com.easy.rpc.example.consumer;


import com.easy.example.common.enity.User;
import com.easy.example.common.service.UserService;
import com.easy.simple.rpc.RpcApplication;
import com.easy.simple.rpc.config.RpcConfig;
import com.easy.simple.rpc.proxy.ServiceProxyFactory;

import static cn.hutool.core.thread.ThreadUtil.sleep;

/**
 * 简易服务消费者示例
 */
public class EasyConsumerExample {

    public static void main(String[] args) {
        // 静态代理
//        UserService userService = new UserServiceProxy();
        // 动态代理
        // 使用json序列化器
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();

        UserService userService = ServiceProxyFactory.getProxy(UserService.class);
        User user = new User();
        user.setName("easy");
        // 调用
        User newUser = userService.getUser(user);
        if (newUser != null) {
            System.out.println(newUser.getName());
        } else {
            System.out.println("user == null");
        }

        UserService userService1 = ServiceProxyFactory.getProxy(UserService.class);
        int number = userService1.getNumber();
        System.out.println(number);
    }
}
