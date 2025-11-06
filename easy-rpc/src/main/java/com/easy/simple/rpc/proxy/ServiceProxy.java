package com.easy.simple.rpc.proxy;


import com.easy.simple.rpc.RpcApplication;
import com.easy.simple.rpc.config.RpcConfig;
import com.easy.simple.rpc.enity.RpcRequest;
import com.easy.simple.rpc.enity.RpcResponse;
import com.easy.simple.rpc.enity.ServiceMetaInfo;
import com.easy.simple.rpc.fault.retry.RetryStrategy;
import com.easy.simple.rpc.fault.retry.RetryStrategyFactory;
import com.easy.simple.rpc.fault.tolerant.TolerantStrategy;
import com.easy.simple.rpc.fault.tolerant.TolerantStrategyFactory;
import com.easy.simple.rpc.loadbalance.LoadBalancer;
import com.easy.simple.rpc.loadbalance.LoadBalancerFactory;
import com.easy.simple.rpc.registry.Registry;
import com.easy.simple.rpc.registry.RegistryFactory;
import com.easy.simple.rpc.serializer.Serializer;
import com.easy.simple.rpc.serializer.SerializerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务代理（JDK 动态代理）
 */
public class ServiceProxy implements InvocationHandler {

    /**
     * 调用代理
     * @param proxy 代理对象
     * @param method 方法
     * @param args 参数
     * @return Object
     * @throws Throwable 调用过程中可能抛出的异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 忽略Object类的方法调用（如toString、equals、hashCode等）,这些方法在调试的时候会自动调用，不需要通过网络发送请求
        if (Object.class.equals(method.getDeclaringClass())) {
            // 如果是本地方法，直接调用，不要通过网络发送请求
            return method.invoke(this, args);
        }

        // 指定序列化器
        Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializerType());

        System.out.println(serializer.getClass().getName());

        // 构造请求
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .serviceVersion("1.0")
                .build();
        // 序列化
        byte[] bodyBytes = serializer.serialize(rpcRequest);
        // 发送请求
        // 从注册中心获取服务地址
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(rpcRequest.getServiceName());
        serviceMetaInfo.setServiceVersion(rpcRequest.getServiceVersion());
        List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());
        System.out.println("从注册中心获取服务地址: " + serviceMetaInfoList);
        if (serviceMetaInfoList.isEmpty()) {
            throw new RuntimeException("暂无服务地址");
        }
        // 负载均衡器
        LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancerType());
        // 使用方法名作为负载均衡的参数
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("methodName", rpcRequest.getMethodName());
        ServiceMetaInfo selectedServiceMetaInfo = loadBalancer.select(requestParams, serviceMetaInfoList);
        RpcResponse retryRpcResponse = null;
        try {
            // 重试策略
            RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(rpcConfig.getRetryStrategyType());
            retryRpcResponse = retryStrategy.doRetry(() ->
                RequestSender.sendRequest(rpcRequest, selectedServiceMetaInfo, serializer)
            );

        } catch (Exception e) {
            // 容错策略
            TolerantStrategy tolerantStrategy = TolerantStrategyFactory.getInstance(rpcConfig.getTolerantStrategyType());
            
            // 构建容错上下文
            Map<String, Object> tolerantContext = new HashMap<>();
            tolerantContext.put("rpcRequest", rpcRequest);
            tolerantContext.put("selectedServiceMetaInfo", selectedServiceMetaInfo);
            tolerantContext.put("serviceMetaInfoList", serviceMetaInfoList);
            tolerantContext.put("methodName", method.getName());
            
            RpcResponse tolerantRpcResponse = tolerantStrategy.doTolerant(tolerantContext, e);
            return tolerantRpcResponse.getData();
        }
        return retryRpcResponse.getData();
    }
}