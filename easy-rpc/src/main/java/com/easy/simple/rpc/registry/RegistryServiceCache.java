package com.easy.simple.rpc.registry;

import com.easy.simple.rpc.enity.ServiceMetaInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注册中心服务本地缓存
 */
public class RegistryServiceCache {

    /**
     * 服务缓存
     */
    Map<String,List<ServiceMetaInfo>> serviceCache = new ConcurrentHashMap<>();

    /**
     * 写缓存
     *
     * @param serviceKey 服务键名
     * @param newServiceCache 新的服务缓存
     */
    public void writeCache(String serviceKey, List<ServiceMetaInfo> newServiceCache) {
        this.serviceCache.put(serviceKey, newServiceCache);
    }

    /**
     * 读缓存
     *
     * @param serviceKey 服务键名
     * @return 服务缓存
     */
    public List<ServiceMetaInfo> readCache(String serviceKey) {
        return this.serviceCache.getOrDefault(serviceKey, new ArrayList<>());
    }
    /**
     * 清空缓存
     *
     * @param serviceKey 服务键名
     */
    public void clearCache(String serviceKey) {
        this.serviceCache.remove(serviceKey);
    }
}
