package com.easy.rpc.example.consumer;

import com.easy.example.common.enity.User;
import com.easy.example.common.service.UserService;
import com.easy.simple.rpc.bootstrap.ConsumerBootstrap;
import com.easy.simple.rpc.proxy.ServiceProxyFactory;

public class ConsumerExample {

    public static void main(String[] args) {
        // 服务提供者初始化
        ConsumerBootstrap.init();

        // 获取代理
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
    }
}
