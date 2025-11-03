package com.easy.simple.rpc.loadbalance;

import lombok.Getter;

public enum LoadBalancerType {
    ConsistentHash(0,"consistentHash","一致性哈希"),
    RANDOM(1, "random", "随机"),
    ROUND_ROBIN(2, "roundRobin", "轮询"),
    LEAST_CONNECTIONS(3, "leastConnections", "最少连接数");

    @Getter
    final int key;
    @Getter
    final String type;
    @Getter
    final String desc;

    LoadBalancerType(int key, String type, String desc) {
        this.key = key;
        this.type = type;
        this.desc = desc;
    }
}
