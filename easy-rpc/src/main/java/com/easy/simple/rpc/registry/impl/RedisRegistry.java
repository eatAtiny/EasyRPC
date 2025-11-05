package com.easy.simple.rpc.registry.impl;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;
import com.easy.simple.rpc.config.RegistryConfig;
import com.easy.simple.rpc.enity.ServiceMetaInfo;
import com.easy.simple.rpc.registry.Registry;
import com.easy.simple.rpc.registry.RegistryServiceCache;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis 注册中心实现
 */
public class RedisRegistry implements Registry {

    /**
     * Redis 连接池
     */
    private JedisPool jedisPool;

    /**
     * Redis 根路径
     */
    private static final String REDIS_ROOT_PATH = "rpc:";

    /**
     * 服务节点过期时间（秒）
     */
    private static final int EXPIRE_TIME = 30;

    /**
     * 本机注册的节点 key 集合（用于维护续期）
     */
    private final Set<String> localRegisterNodeKeySet = new HashSet<>();

    /**
     * 注册中心服务缓存
     */
    private final RegistryServiceCache registryServiceCache = new RegistryServiceCache();

    /**
     * 监听服务集合
     */
    private final Set<String> watchingServiceKeySet = new ConcurrentHashSet<>();

    /**
     * 初始化注册中心
     * @param registryConfig 注册中心配置
     */
    @Override
    public void init(RegistryConfig registryConfig) {
        // 解析Redis地址
        String address = registryConfig.getAddress();
        String[] parts = address.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6379;

        // 创建连接池配置
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxIdle(20);
        poolConfig.setMinIdle(5);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);

        // 创建连接池
        jedisPool = new JedisPool(poolConfig, host, port, 2000);

        // 启动心跳检测
        heartBeat();
    }

    @Override
    public void register(ServiceMetaInfo serviceMetaInfo) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            // 设置服务节点键
            String registerKey = REDIS_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
            
            // 使用SET命令设置键值对，并设置过期时间
            jedis.setex(registerKey, EXPIRE_TIME, JSONUtil.toJsonStr(serviceMetaInfo));

            // 同时将服务节点添加到服务集合中
            String serviceSetKey = REDIS_ROOT_PATH + "services:" + serviceMetaInfo.getServiceKey();
            jedis.sadd(serviceSetKey, registerKey);
            jedis.expire(serviceSetKey, EXPIRE_TIME);

            // 加入本地注册节点 key 集合
            localRegisterNodeKeySet.add(registerKey);
            
            // 发布服务更新消息
            String message = serviceMetaInfo.getServiceKey() + ":UPDATE";
            jedis.publish(SERVICE_CHANGE_CHANNEL, message);
        }
    }

    @Override
    public void unRegister(ServiceMetaInfo serviceMetaInfo) {
        try (Jedis jedis = jedisPool.getResource()) {
            String registerKey = REDIS_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
            
            // 删除服务节点
            jedis.del(registerKey);
            
            // 从服务集合中移除
            String serviceSetKey = REDIS_ROOT_PATH + "services:" + serviceMetaInfo.getServiceKey();
            jedis.srem(serviceSetKey, registerKey);

            // 从本地注册节点 key 集合移除
            localRegisterNodeKeySet.remove(registerKey);
            
            // 发布服务删除消息
            String message = serviceMetaInfo.getServiceKey() + ":DELETE";
            jedis.publish(SERVICE_CHANGE_CHANNEL, message);
        }
    }

    @Override
    public List<ServiceMetaInfo> serviceDiscovery(String serviceKey) {
        // 从缓存中读取服务
        List<ServiceMetaInfo> cachedServiceMetaInfoList = registryServiceCache.readCache(serviceKey);
        if (!cachedServiceMetaInfoList.isEmpty()) {
            return cachedServiceMetaInfoList;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String serviceSetKey = REDIS_ROOT_PATH + "services:" + serviceKey;
            
            // 获取服务集合中的所有节点键
            Set<String> nodeKeys = jedis.smembers(serviceSetKey);
            
            if (nodeKeys == null || nodeKeys.isEmpty()) {
                return new java.util.ArrayList<>();
            }

            // 获取所有节点的服务信息
            List<ServiceMetaInfo> serviceMetaInfoList = nodeKeys.stream()
                    .map(nodeKey -> {
                        // 监听服务变化
                        watch(serviceKey);
                        
                        String value = jedis.get(nodeKey);
                        if (value != null) {
                            return JSONUtil.toBean(value, ServiceMetaInfo.class);
                        }
                        return null;
                    })
                    .filter(serviceMetaInfo -> serviceMetaInfo != null)
                    .collect(Collectors.toList());

            // 写入缓存
            System.out.println("服务节点 " + serviceKey + " 被发现");
            registryServiceCache.writeCache(serviceKey, serviceMetaInfoList);
            return serviceMetaInfoList;
        }
    }

    @Override
    public void heartBeat() {
        // 12秒续签一次
        CronUtil.schedule("*/12 * * * * ?", new Task(){
            @Override
            public void execute() {
                // 遍历本节点所有注册服务
                for (String localRegisterNodeKey : localRegisterNodeKeySet) {
                    try (Jedis jedis = jedisPool.getResource()) {
                        // 检查节点是否存在
                        if (jedis.exists(localRegisterNodeKey)) {
                            // 节点存在，续签
                            String value = jedis.get(localRegisterNodeKey);
                            if (value != null) {
                                ServiceMetaInfo serviceMetaInfo = JSONUtil.toBean(value, ServiceMetaInfo.class);
                                register(serviceMetaInfo);
                                
                                // 发布心跳续签消息
                                String message = serviceMetaInfo.getServiceKey() + ":HEARTBEAT";
                                jedis.publish(SERVICE_CHANGE_CHANNEL, message);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println(localRegisterNodeKey + "续签失败: " + e.getMessage());
                    }
                }
            }
        });
        CronUtil.setMatchSecond(true);
        // 启动定时任务
        CronUtil.start();
    }

    /**
     * Redis服务监听器
     */
    private static class ServiceWatcher extends JedisPubSub {
        private final RegistryServiceCache registryServiceCache;
        
        public ServiceWatcher(RegistryServiceCache registryServiceCache) {
            this.registryServiceCache = registryServiceCache;
        }
        
        @Override
        public void onMessage(String channel, String message) {
            // 解析消息格式：serviceKey:action
            String[] parts = message.split(":");
            if (parts.length == 2) {
                String serviceKey = parts[0];
                String action = parts[1];
                
                if ("DELETE".equals(action)) {
                    // 服务节点被删除，清理缓存
                    System.out.println("服务节点 " + serviceKey + " 下线");
                    registryServiceCache.clearCache(serviceKey);
                } else if ("UPDATE".equals(action)) {
                    // 服务节点更新，清理缓存以便下次重新发现
                    System.out.println("服务节点 " + serviceKey + " 更新");
                    registryServiceCache.clearCache(serviceKey);
                }
            }
        }
        
        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            System.out.println("开始监听服务变化频道: " + channel);
        }
        
        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {
            System.out.println("停止监听服务变化频道: " + channel);
        }
    }
    
    /**
     * 服务变化监听频道
     */
    private static final String SERVICE_CHANGE_CHANNEL = "rpc:service:changes";
    
    /**
     * 服务监听器实例
     */
    private ServiceWatcher serviceWatcher;
    
    /**
     * 监听线程
     */
    private Thread watchThread;

    /**
     * 监听服务变化（使用Redis发布订阅模式实现监听）
     * @param serviceKey 服务键名
     */
    @Override
    public void watch(String serviceKey) {
        boolean newWatch = watchingServiceKeySet.add(serviceKey);
        if (newWatch) {
            // 启动监听线程（如果尚未启动）
            startWatcher();
            
            // 发布服务开始监听的消息
            try (Jedis jedis = jedisPool.getResource()) {
                String message = serviceKey + ":WATCH_START";
                jedis.publish(SERVICE_CHANGE_CHANNEL, message);
            }
            
            System.out.println("开始监听服务: " + serviceKey);
        }
    }
    
    /**
     * 启动服务监听器
     */
    private void startWatcher() {
        if (serviceWatcher == null) {
            serviceWatcher = new ServiceWatcher(registryServiceCache);
            
            watchThread = new Thread(() -> {
                try (Jedis jedis = jedisPool.getResource()) {
                    // 订阅服务变化频道
                    jedis.subscribe(serviceWatcher, SERVICE_CHANGE_CHANNEL);
                } catch (Exception e) {
                    System.err.println("服务监听线程异常: " + e.getMessage());
                }
            });
            
            watchThread.setDaemon(true);
            watchThread.setName("Redis-Service-Watcher");
            watchThread.start();
        }
    }

    @Override
    public void destroy() {
        System.out.println("当前节点下线");
        
        // 停止监听线程
        if (serviceWatcher != null && watchThread != null && watchThread.isAlive()) {
            serviceWatcher.unsubscribe(SERVICE_CHANGE_CHANNEL);
            watchThread.interrupt();
        }
        
        // 清理所有本地注册的服务
        for (String localRegisterNodeKey : localRegisterNodeKeySet) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del(localRegisterNodeKey);
                
                // 发布服务删除消息
                String serviceKey = extractServiceKeyFromNodeKey(localRegisterNodeKey);
                if (serviceKey != null) {
                    String message = serviceKey + ":DELETE";
                    jedis.publish(SERVICE_CHANGE_CHANNEL, message);
                }
            }
        }
        
        // 关闭连接池
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
    
    /**
     * 从节点键中提取服务键
     * @param nodeKey 节点键（格式：rpc:{serviceKey}/{address}:{port}）
     * @return 服务键
     */
    private String extractServiceKeyFromNodeKey(String nodeKey) {
        if (nodeKey.startsWith(REDIS_ROOT_PATH)) {
            String withoutPrefix = nodeKey.substring(REDIS_ROOT_PATH.length());
            int slashIndex = withoutPrefix.indexOf('/');
            if (slashIndex > 0) {
                return withoutPrefix.substring(0, slashIndex);
            }
        }
        return null;
    }
}