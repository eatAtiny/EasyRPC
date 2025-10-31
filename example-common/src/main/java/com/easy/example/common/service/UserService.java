package com.easy.example.common.service;


import com.easy.example.common.enity.User;

/**
 * 用户服务
 */
public interface UserService {

    /**
     * 获取用户
     *
     * @param user  用户
     * @return  用户
     */
    User getUser(User user);

     /**
      * 获取数字
      *
      * @return  数字
      */
     int getNumber();
}
