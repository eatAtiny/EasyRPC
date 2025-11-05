package com.easy.rpc.springprovider;

import com.easy.example.common.enity.User;
import com.easy.example.common.service.UserService;
import com.easy.simple.rpc.annotation.RpcService;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 */
@Service
@RpcService
public class UserServiceImpl implements UserService {

    @Override
    public User getUser(User user) {
        System.out.println("用户名：" + user.getName());
        return user;
    }

    @Override
    public int getNumber() {
        return 100;
    }


}
