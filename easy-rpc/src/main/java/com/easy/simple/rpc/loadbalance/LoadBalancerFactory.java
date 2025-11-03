package com.easy.simple.rpc.loadbalance;

import com.easy.simple.rpc.loadbalance.impl.RoundRobinLoadBalancer;
import com.easy.simple.rpc.utils.SpiLoader;

/**
 * 负载均衡器工厂（工厂模式，用于获取负载均衡器对象）
 */
public class LoadBalancerFactory {

    static {
        SpiLoader.load(LoadBalancer.class);
    }

    /**
     * 默认负载均衡器
     */
    private static final LoadBalancer DEFAULT_LOAD_BALANCER = new RoundRobinLoadBalancer();

    /**
     * 获取实例
     *
     * @param type 负载均衡器类型
     * @return 负载均衡器实例
     */
    public static LoadBalancer getInstance(String type) {
        return SpiLoader.getInstance(LoadBalancer.class, type);
    }

}
