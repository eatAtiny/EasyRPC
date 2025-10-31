package com.easy.rpc.example.provider;


import com.easy.example.common.enity.User;
import com.easy.example.common.service.UserService;

/**
 * 用户服务实现类
 */
public class UserServiceImpl implements UserService {

    public User getUser(User user) {
        System.out.println("用户名：" + user.getName());
        user.setName("我是" + user.getName());
        return user;
    }

    @Override
    public int getNumber() {
        return 100;
    }
}
