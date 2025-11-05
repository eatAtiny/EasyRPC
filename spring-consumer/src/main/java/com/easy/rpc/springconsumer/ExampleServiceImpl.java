package com.easy.rpc.springconsumer;

import com.easy.example.common.enity.User;
import com.easy.example.common.service.UserService;
import com.easy.simple.rpc.annotation.RpcReference;
import org.springframework.stereotype.Service;

@Service
public class ExampleServiceImpl {

    @RpcReference
    private UserService userService;

    public void test() {
        User user = new User();
        user.setName("easy");
        User resultUser = userService.getUser(user);
        System.out.println(resultUser.getName());
    }

}
