package com.easy.simple.rpc.server;

import lombok.Getter;

public enum ServerType {
    HTTP(0, "http"),
    TCP(1, "tcp");

    @Getter
    private final int key;
    @Getter
    private final String type;

    ServerType(int key, String type) {
        this.key = key;
        this.type = type;
    }

}
