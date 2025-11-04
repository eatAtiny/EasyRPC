package com.easy.simple.rpc.fault.tolerant.impl;

import com.easy.simple.rpc.RpcApplication;
import com.easy.simple.rpc.config.RpcConfig;
import com.easy.simple.rpc.enity.RpcRequest;
import com.easy.simple.rpc.enity.RpcResponse;
import com.easy.simple.rpc.enity.ServiceMetaInfo;
import com.easy.simple.rpc.fault.tolerant.TolerantStrategy;
import com.easy.simple.rpc.fault.tolerant.TolerantStrategyFactory;
import com.easy.simple.rpc.loadbalance.LoadBalancer;
import com.easy.simple.rpc.loadbalance.LoadBalancerFactory;
import com.easy.simple.rpc.registry.Registry;
import com.easy.simple.rpc.registry.RegistryFactory;
import com.easy.simple.rpc.serializer.Serializer;
import com.easy.simple.rpc.serializer.SerializerFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 降级到其他服务 - 容错策略
 * 当重试策略失败时，自动发现并使用其他容错策略
 */
@Slf4j
public class FailBackTolerantStrategy implements TolerantStrategy {

    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        log.warn("重试策略失败，开始尝试其他容错策略，异常信息: {}", e.getMessage());
        
        // 从上下文中获取必要的信息
        if (context == null) {
            log.error("容错上下文为空，无法进行策略切换");
            return createErrorResponse("容错上下文为空");
        }
        
        RpcRequest rpcRequest = (RpcRequest) context.get("rpcRequest");
        if (rpcRequest == null) {
            log.error("RPC请求信息为空，无法进行策略切换");
            return createErrorResponse("RPC请求信息为空");
        }
        
        // 获取配置
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        
        // 获取所有可用的容错策略类型
        String[] availableStrategies = {"failSafe", "failFast", "failOver"};
        String currentStrategy = rpcConfig.getTolerantStrategyType();
        
        log.info("当前容错策略: {}，开始尝试其他策略", currentStrategy);
        
        // 尝试其他容错策略
        for (String strategyType : availableStrategies) {
            if (!strategyType.equals(currentStrategy)) {
                try {
                    log.info("尝试容错策略: {}", strategyType);
                    TolerantStrategy strategy = TolerantStrategyFactory.getInstance(strategyType);
                    RpcResponse response = strategy.doTolerant(context, e);
                    
                    if (response != null) {
                        log.info("容错策略 {} 执行成功", strategyType);
                        return response;
                    }
                    
                    log.warn("容错策略 {} 执行失败", strategyType);
                } catch (Exception ex) {
                    log.warn("容错策略 {} 执行异常: {}", strategyType, ex.getMessage());
                }
            }
        }
        
        // 如果所有策略都失败，尝试备用服务降级
        log.info("所有容错策略均失败，尝试备用服务降级");
        return tryBackupServiceFallback(rpcRequest, context, e);
    }
    
    /**
     * 尝试备用服务降级
     */
    private RpcResponse tryBackupServiceFallback(RpcRequest rpcRequest, Map<String, Object> context, Exception originalException) {
        log.info("开始备用服务降级处理");
        
        try {
            // 查找备用服务
            List<ServiceMetaInfo> backupServices = findBackupServices(rpcRequest, context);
            if (backupServices.isEmpty()) {
                log.warn("未找到可用的备用服务");
                return createErrorResponse("未找到可用的备用服务");
            }
            
            // 使用负载均衡选择备用服务
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancerType());
            ServiceMetaInfo backupService = loadBalancer.select(context, backupServices);
            
            if (backupService == null) {
                log.warn("负载均衡器未选择到备用服务");
                return createErrorResponse("负载均衡器未选择到备用服务");
            }
            
            log.info("降级到备用服务: {}", backupService.getServiceAddress());
            
            // 调用备用服务
            return callBackupService(rpcRequest, backupService);
            
        } catch (Exception e) {
            log.error("备用服务降级过程中发生异常: {}", e.getMessage(), e);
            return createErrorResponse("备用服务降级失败: " + e.getMessage());
        }
    }
    
    /**
     * 查找备用服务
     */
    private List<ServiceMetaInfo> findBackupServices(RpcRequest rpcRequest, Map<String, Object> context) {
        Registry registry = RegistryFactory.getInstance(RpcApplication.getRpcConfig().getRegistryConfig().getRegistry());
        List<ServiceMetaInfo> backupServices = new ArrayList<>();
        
        // 策略1：查找命名后缀的备用服务
        String backupServiceName = rpcRequest.getServiceName() + ".backup";
        ServiceMetaInfo backupServiceMetaInfo = new ServiceMetaInfo();
        backupServiceMetaInfo.setServiceName(backupServiceName);
        backupServiceMetaInfo.setServiceVersion(rpcRequest.getServiceVersion());
        
        List<ServiceMetaInfo> namedBackupServices = registry.serviceDiscovery(backupServiceMetaInfo.getServiceKey());
        if (!namedBackupServices.isEmpty()) {
            backupServices.addAll(namedBackupServices);
            log.info("找到命名备用服务: {}，数量: {}", backupServiceMetaInfo.getServiceKey(), namedBackupServices.size());
        }
        
        // 策略2：查找同服务的其他实例
        if (backupServices.isEmpty()) {
            ServiceMetaInfo originalServiceMetaInfo = new ServiceMetaInfo();
            originalServiceMetaInfo.setServiceName(rpcRequest.getServiceName());
            originalServiceMetaInfo.setServiceVersion(rpcRequest.getServiceVersion());
            
            List<ServiceMetaInfo> originalServices = registry.serviceDiscovery(originalServiceMetaInfo.getServiceKey());
            
            // 排除当前失败的服务
            ServiceMetaInfo failedService = (ServiceMetaInfo) context.get("selectedServiceMetaInfo");
            if (failedService != null && originalServices.size() > 1) {
                originalServices.removeIf(service -> 
                    service.getServiceAddress().equals(failedService.getServiceAddress()));
                log.info("排除失败服务: {}，剩余服务数量: {}", failedService.getServiceAddress(), originalServices.size());
            }
            
            if (!originalServices.isEmpty()) {
                backupServices.addAll(originalServices);
                log.info("找到同服务其他实例: {}，数量: {}", originalServiceMetaInfo.getServiceKey(), originalServices.size());
            }
        }
        
        if (backupServices.isEmpty()) {
            log.warn("未找到任何可用的备用服务");
        }
        
        return backupServices;
    }
    
    /**
     * 调用备用服务
     */
    private RpcResponse callBackupService(RpcRequest rpcRequest, ServiceMetaInfo backupService) {
        try {
            log.info("调用备用服务: {} - {}", backupService.getServiceAddress(), rpcRequest.getMethodName());
            
            // 获取配置
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            
            // 使用与主服务相同的序列化器
            Serializer serializer = SerializerFactory.getInstance(rpcConfig.getSerializerType());
            byte[] bodyBytes = serializer.serialize(rpcRequest);
            
            // 根据备用服务的协议类型选择调用方式
            if (backupService.getServiceAddress().startsWith("http://")) {
                // HTTP协议调用
                return callBackupServiceViaHttp(rpcRequest, backupService, bodyBytes, serializer);
            } else {
                // TCP协议调用（与主服务相同的调用方式）
                return callBackupServiceViaTcp(rpcRequest, backupService, bodyBytes, serializer);
            }
            
        } catch (Exception e) {
            log.error("调用备用服务失败: {}", e.getMessage(), e);
            throw new RuntimeException("备用服务调用失败", e);
        }
    }
    
    /**
     * 通过HTTP协议调用备用服务
     */
    private RpcResponse callBackupServiceViaHttp(RpcRequest rpcRequest, ServiceMetaInfo backupService, 
                                                 byte[] bodyBytes, Serializer serializer) {
        try {
            // 使用Hutool的HttpRequest进行HTTP调用
            cn.hutool.http.HttpResponse httpResponse = cn.hutool.http.HttpRequest
                    .post(backupService.getServiceAddress() + "/rpc")
                    .body(bodyBytes)
                    .header("Content-Type", "application/octet-stream")
                    .header("Rpc-Service-Name", rpcRequest.getServiceName())
                    .header("Rpc-Service-Version", rpcRequest.getServiceVersion())
                    .timeout(5000) // 5秒超时
                    .execute();
            
            if (httpResponse.isOk()) {
                byte[] responseBytes = httpResponse.bodyBytes();
                RpcResponse rpcResponse = serializer.deserialize(responseBytes, RpcResponse.class);
//                rpcResponse.setSuccess(true);
                return rpcResponse;
            } else {
                log.error("HTTP调用备用服务失败，状态码: {}", httpResponse.getStatus());
                return createErrorResponse("HTTP调用备用服务失败，状态码: " + httpResponse.getStatus());
            }
        } catch (Exception e) {
            log.error("HTTP调用备用服务异常: {}", e.getMessage());
            return createErrorResponse("HTTP调用备用服务异常: " + e.getMessage());
        }
    }
    
    /**
     * 通过TCP协议调用备用服务（与主服务相同的调用逻辑）
     */
    private RpcResponse callBackupServiceViaTcp(RpcRequest rpcRequest, ServiceMetaInfo backupService, 
                                                byte[] bodyBytes, Serializer serializer) {
        try {
            // 解析服务地址
            String[] addressParts = backupService.getServiceAddress().split(":");
            if (addressParts.length != 2) {
                throw new IllegalArgumentException("无效的服务地址格式: " + backupService.getServiceAddress());
            }
            
            String host = addressParts[0];
            int port = Integer.parseInt(addressParts[1]);
            
            // 使用Vert.x进行TCP调用
            io.vertx.core.Vertx vertx = io.vertx.core.Vertx.vertx();
            io.vertx.core.net.NetClient netClient = vertx.createNetClient();
            
            java.util.concurrent.CompletableFuture<RpcResponse> responseFuture = new java.util.concurrent.CompletableFuture<>();
            
            netClient.connect(port, host, result -> {
                if (result.succeeded()) {
                    io.vertx.core.net.NetSocket netSocket = result.result();
                    
                    // 构造协议消息
                    com.easy.simple.rpc.protocol.ProtocolMessage<RpcRequest> protocolMessage = new com.easy.simple.rpc.protocol.ProtocolMessage<>();
                    com.easy.simple.rpc.protocol.ProtocolMessage.Header header = new com.easy.simple.rpc.protocol.ProtocolMessage.Header();
                    header.setMagic(com.easy.simple.rpc.protocol.ProtocolConstant.PROTOCOL_MAGIC);
                    header.setVersion(com.easy.simple.rpc.protocol.ProtocolConstant.PROTOCOL_VERSION);
                    header.setSerializer((byte) com.easy.simple.rpc.serializer.SerializerType.getKeyByType(RpcApplication.getRpcConfig().getSerializerType()));
                    header.setType((byte) com.easy.simple.rpc.protocol.ProtocolMessageTypeEnum.REQUEST.getKey());
                    header.setRequestId(System.currentTimeMillis());
                    header.setBodyLength(bodyBytes.length);
                    protocolMessage.setHeader(header);
                    protocolMessage.setBody(rpcRequest);
                    
                    try {
                        // 编码并发送
                        io.vertx.core.buffer.Buffer encodedBuffer = com.easy.simple.rpc.protocol.CompactProtocolCodec.encode(protocolMessage);
                        netSocket.write(encodedBuffer);
                        
                        // 设置响应处理器
                        netSocket.handler(buffer -> {
                            try {
                                // 解码响应
                                com.easy.simple.rpc.protocol.ProtocolMessage<?> responseMessage = 
                                    com.easy.simple.rpc.protocol.CompactProtocolCodec.decode(buffer);
                                
                                if (responseMessage.getBody() instanceof RpcResponse) {
                                    RpcResponse rpcResponse = (RpcResponse) responseMessage.getBody();
                                    responseFuture.complete(rpcResponse);
                                } else {
                                    responseFuture.completeExceptionally(new RuntimeException("响应消息体类型不匹配"));
                                }
                            } catch (Exception e) {
                                responseFuture.completeExceptionally(e);
                            } finally {
                                netSocket.close();
                            }
                        });
                        
                        // 设置超时
                        vertx.setTimer(5000, timerId -> {
                            if (!responseFuture.isDone()) {
                                responseFuture.completeExceptionally(new RuntimeException("备用服务调用超时"));
                                netSocket.close();
                            }
                        });
                        
                    } catch (Exception e) {
                        responseFuture.completeExceptionally(e);
                        netSocket.close();
                    }
                } else {
                    responseFuture.completeExceptionally(result.cause());
                }
            });
            
            RpcResponse response = responseFuture.get();
            netClient.close();
            return response;
            
        } catch (Exception e) {
            log.error("TCP调用备用服务失败: {}", e.getMessage());
            return createErrorResponse("TCP调用备用服务失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建错误响应
     */
    private RpcResponse createErrorResponse(String message) {
        RpcResponse response = new RpcResponse();
//        response.setSuccess(false);
        response.setMessage(message);
        response.setException(new RuntimeException(message));
        return response;
    }
    
    /**
     * 获取备用服务调用器（可根据需要扩展）
     */
    protected Object getBackupServiceInvoker() {
        // 这里可以返回一个预定义的备用服务调用器
        // 例如：本地存根、mock服务、简化版服务等
        return null;
    }
}