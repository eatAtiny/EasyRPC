package com.easy.simple.rpc.registry;

public enum RegistryType {
    ETCD("etcd"),
    ZOOKEEPER("zookeeper");

    private final String type;

    RegistryType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
