package com.easy.simple.rpc.registry;


import com.easy.simple.rpc.config.RegistryConfig;
import com.easy.simple.rpc.enity.ServiceMetaInfo;

import java.util.List;

/**
 * 注册中心接口
 */
public interface Registry {

    /**
     * 初始化
     *
     * @param registryConfig 注册中心配置
     */
    void init(RegistryConfig registryConfig);

    /**
     * 注册服务（服务端）
     *
     * @param serviceMetaInfo 服务元信息
     */
    void register(ServiceMetaInfo serviceMetaInfo) throws Exception;

    /**
     * 注销服务（服务端）
     *
     * @param serviceMetaInfo 服务元信息
     */
    void unRegister(ServiceMetaInfo serviceMetaInfo);

    /**
     * 心跳检测（服务端）
     */
    void heartBeat();

    /**
     * 服务发现（获取某服务的所有节点，消费端）
     *
     * @param serviceKey 服务键名
     * @return 服务节点列表
     */
    List<ServiceMetaInfo> serviceDiscovery(String serviceKey);

    /**
     * 监听服务（消费端）
     *
     * @param serviceKey 服务键名
     */
    void watch(String serviceKey);

    /**
     * 服务销毁
     */
    void destroy();
}
