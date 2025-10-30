package com.easy.example.common.enity;

import java.io.Serializable;

/**
 * 用户
 */
public class User implements Serializable {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
