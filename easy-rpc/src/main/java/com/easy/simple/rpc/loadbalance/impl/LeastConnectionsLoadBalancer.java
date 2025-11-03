package com.easy.simple.rpc.loadbalance.impl;

import com.easy.simple.rpc.enity.ServiceMetaInfo;
import com.easy.simple.rpc.loadbalance.LoadBalancer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 最少连接数负载均衡器
 * 选择当前连接数最少的服务节点进行连接
 */
public class LeastConnectionsLoadBalancer implements LoadBalancer {

    /**
     * 存储每个服务节点的连接数
     * key: 服务地址 (host:port)
     * value: 当前连接数
     */
    private final Map<String, AtomicInteger> connectionCountMap = new ConcurrentHashMap<>();

    @Override
    public ServiceMetaInfo select(Map<String, Object> requestParams, List<ServiceMetaInfo> serviceMetaInfoList) {
        if (serviceMetaInfoList.isEmpty()) {
            return null;
        }

        // 如果只有一个节点，直接返回
        if (serviceMetaInfoList.size() == 1) {
            ServiceMetaInfo serviceMetaInfo = serviceMetaInfoList.get(0);
            incrementConnectionCount(serviceMetaInfo);
            return serviceMetaInfo;
        }

        // 找到连接数最少的节点
        ServiceMetaInfo selectedService = null;
        int minConnections = Integer.MAX_VALUE;

        for (ServiceMetaInfo serviceMetaInfo : serviceMetaInfoList) {
            String serviceAddress = serviceMetaInfo.getServiceAddress();
            int currentConnections = getConnectionCount(serviceAddress);

            if (currentConnections < minConnections) {
                minConnections = currentConnections;
                selectedService = serviceMetaInfo;
            }
        }

        // 增加选中节点的连接数
        if (selectedService != null) {
            incrementConnectionCount(selectedService);
        }

        return selectedService;
    }

    /**
     * 获取服务节点的连接数
     * @param serviceAddress 服务地址
     * @return 连接数
     */
    private int getConnectionCount(String serviceAddress) {
        return connectionCountMap.getOrDefault(serviceAddress, new AtomicInteger(0)).get();
    }

    /**
     * 增加服务节点的连接数
     * @param serviceMetaInfo 服务元信息
     */
    private void incrementConnectionCount(ServiceMetaInfo serviceMetaInfo) {
        String serviceAddress = serviceMetaInfo.getServiceAddress();
        AtomicInteger connectionCount = connectionCountMap.computeIfAbsent(
            serviceAddress, k -> new AtomicInteger(0)
        );
        connectionCount.incrementAndGet();
    }

    /**
     * 减少服务节点的连接数（当连接关闭时调用）
     * @param serviceMetaInfo 服务元信息
     */
    public void decrementConnectionCount(ServiceMetaInfo serviceMetaInfo) {
        String serviceAddress = serviceMetaInfo.getServiceAddress();
        AtomicInteger connectionCount = connectionCountMap.get(serviceAddress);
        if (connectionCount != null && connectionCount.get() > 0) {
            connectionCount.decrementAndGet();
        }
    }

    /**
     * 获取所有服务节点的连接数统计（用于监控）
     * @return 连接数统计Map
     */
    public Map<String, Integer> getConnectionStatistics() {
        Map<String, Integer> statistics = new ConcurrentHashMap<>();
        connectionCountMap.forEach((address, count) -> 
            statistics.put(address, count.get())
        );
        return statistics;
    }

    /**
     * 清空连接数统计（用于服务重启或重置）
     */
    public void clearConnectionStatistics() {
        connectionCountMap.clear();
    }
}