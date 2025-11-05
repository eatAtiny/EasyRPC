package com.easy.simple.rpc.constant;

/**
 * RPC 相关常量
 */
public interface RpcConstant {

    /**
     * 默认配置文件加载前缀
     */
    String DEFAULT_CONFIG_PREFIX = "rpc";

    /**
     * 默认服务版本
     */
    String DEFAULT_SERVICE_VERSION = "1.0";

    /**
     * 默认负载均衡器
     */
    String DEFAULT_LOAD_BALANCER = "roundRobin";

    /**
     * 默认重试策略
     */
    String DEFAULT_RETRY_STRATEGY = "no";

     /**
      * 默认容错策略
      */
    String DEFAULT_TOLERANT_STRATEGY = "failFast";
}
