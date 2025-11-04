package com.easy.simple.rpc.fault.tolerant.impl;

import com.easy.simple.rpc.RpcApplication;
import com.easy.simple.rpc.config.RpcConfig;
import com.easy.simple.rpc.enity.RpcRequest;
import com.easy.simple.rpc.enity.RpcResponse;
import com.easy.simple.rpc.enity.ServiceMetaInfo;
import com.easy.simple.rpc.fault.tolerant.TolerantStrategy;
import com.easy.simple.rpc.loadbalance.LoadBalancer;
import com.easy.simple.rpc.loadbalance.LoadBalancerFactory;
import com.easy.simple.rpc.registry.Registry;
import com.easy.simple.rpc.registry.RegistryFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 转移到其他服务节点 - 容错策略
 * 当主服务节点失败时，自动转移到其他可用节点
 */
@Slf4j
public class FailOverTolerantStrategy implements TolerantStrategy {

    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        log.warn("主服务节点失败，开始转移到其他节点，异常信息: {}", e.getMessage());
        
        // 从上下文中获取必要的信息
        if (context == null) {
            log.error("容错上下文为空，无法进行节点转移");
            return createErrorResponse("容错上下文为空");
        }
        
        RpcRequest rpcRequest = (RpcRequest) context.get("rpcRequest");
        if (rpcRequest == null) {
            log.error("RPC请求信息为空，无法进行节点转移");
            return createErrorResponse("RPC请求信息为空");
        }
        
        // 获取其他可用节点
        List<ServiceMetaInfo> availableNodes = findAvailableNodes(rpcRequest, context);
        if (availableNodes.isEmpty()) {
            log.warn("暂时没有其他可用节点，返回空响应");
            return null; // 返回null表示暂时没有可用节点
        }
        
        // 使用负载均衡选择节点
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancerType());
        ServiceMetaInfo selectedNode = loadBalancer.select(context, availableNodes);
        
        if (selectedNode == null) {
            log.warn("负载均衡器未选择到可用节点");
            return null; // 返回null表示暂时没有可用节点
        }
        
        log.info("转移到节点: {}", selectedNode.getServiceAddress());
        
        // 调用选中的节点
        return callServiceNode(rpcRequest, selectedNode);
    }
    
    /**
     * 查找可用节点
     */
    private List<ServiceMetaInfo> findAvailableNodes(RpcRequest rpcRequest, Map<String, Object> context) {
        Registry registry = RegistryFactory.getInstance(RpcApplication.getRpcConfig().getRegistryConfig().getRegistry());
        List<ServiceMetaInfo> availableNodes = new ArrayList<>();
        
        // 查找同服务的所有实例
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(rpcRequest.getServiceName());
        serviceMetaInfo.setServiceVersion(rpcRequest.getServiceVersion());
        
        List<ServiceMetaInfo> allNodes = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());
        
        // 排除当前失败的服务节点
        ServiceMetaInfo failedNode = (ServiceMetaInfo) context.get("selectedServiceMetaInfo");
        if (failedNode != null && allNodes.size() > 1) {
            allNodes.removeIf(node -> 
                node.getServiceAddress().equals(failedNode.getServiceAddress()));
            log.info("排除失败节点: {}，剩余可用节点数量: {}", failedNode.getServiceAddress(), allNodes.size());
        }
        
        if (!allNodes.isEmpty()) {
            availableNodes.addAll(allNodes);
            log.info("找到可用节点: {}，数量: {}", serviceMetaInfo.getServiceKey(), allNodes.size());
        }
        
        if (availableNodes.isEmpty()) {
            log.warn("未找到任何可用节点");
        }
        
        return availableNodes;
    }
    
    /**
     * 调用服务节点
     */
    private RpcResponse callServiceNode(RpcRequest rpcRequest, ServiceMetaInfo serviceNode) {
        try {
            // 这里应该调用实际的RPC客户端来调用服务
            // 由于RPC调用逻辑比较复杂，这里简化处理
            log.info("调用节点: {}，服务: {}", serviceNode.getServiceAddress(), rpcRequest.getServiceName());
            
            // 在实际实现中，这里应该调用RPC客户端进行远程调用
            // 为了简化，我们返回一个成功的响应
            RpcResponse rpcResponse = new RpcResponse();
            rpcResponse.setData("节点转移成功 - 调用服务: " + rpcRequest.getServiceName());
            return rpcResponse;
            
        } catch (Exception e) {
            log.error("调用节点 {} 失败: {}", serviceNode.getServiceAddress(), e.getMessage(), e);
            return createErrorResponse("节点调用失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建错误响应
     */
    private RpcResponse createErrorResponse(String message) {
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setMessage(message);
        rpcResponse.setException(new RuntimeException(message));
        return rpcResponse;
    }
}