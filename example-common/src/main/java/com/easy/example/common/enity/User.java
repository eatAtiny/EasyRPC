package com.easy.example.common.enity;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户
 */
@Data
public class User implements Serializable {

    private String name;

    private int age;

    private String email;
}
