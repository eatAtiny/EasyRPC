package com.easy.simple.rpc.registry;

public enum RegistryType {
    ETCD("etcd","et", "etcd 注册中心"),
    ZOOKEEPER("zookeeper", "zk", "zookeeper 注册中心"),
    REDIS("redis", "rd", "redis 注册中心");

    private final String type;
    private final String prefix;
    private final String description;

    RegistryType(String type, String prefix, String description) {
        this.type = type;
        this.prefix = prefix;
        this.description = description;
    }
    public String getPrefix() {
        return prefix;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}