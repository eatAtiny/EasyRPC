package com.easy.simple.rpc.proxy;


import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.easy.simple.rpc.RpcApplication;
import com.easy.simple.rpc.config.RpcConfig;
import com.easy.simple.rpc.enity.RpcRequest;
import com.easy.simple.rpc.enity.RpcResponse;
import com.easy.simple.rpc.enity.ServiceMetaInfo;
import com.easy.simple.rpc.fault.retry.RetryStrategy;
import com.easy.simple.rpc.fault.retry.RetryStrategyFactory;
import com.easy.simple.rpc.loadbalance.LoadBalancer;
import com.easy.simple.rpc.loadbalance.LoadBalancerFactory;
import com.easy.simple.rpc.protocol.CompactProtocolCodec;
import com.easy.simple.rpc.protocol.ProtocolConstant;
import com.easy.simple.rpc.protocol.ProtocolMessage;
import com.easy.simple.rpc.protocol.ProtocolMessageTypeEnum;
import com.easy.simple.rpc.protocol.TcpPacketDecoder;
import com.easy.simple.rpc.registry.Registry;
import com.easy.simple.rpc.registry.RegistryFactory;
import com.easy.simple.rpc.serializer.Serializer;
import com.easy.simple.rpc.serializer.SerializerFactory;
import com.easy.simple.rpc.serializer.SerializerType;
import io.grpc.protobuf.ProtoMethodDescriptorSupplier;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
        try {
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
//            // 发送Http请求
//            try (HttpResponse httpResponse = HttpRequest.post(selectedServiceMetaInfo.getServiceAddress())
//                    .body(bodyBytes)
//                    .execute()) {
//                byte[] result = httpResponse.bodyBytes();
//                // 反序列化
//                RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
//                return rpcResponse.getData();
//            }
            // 重试策略
            RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(rpcConfig.getRetryStrategyType());
            RpcResponse retryRpcResponse = retryStrategy.doRetry(() ->
            {
                // 发送tcp请求
                Vertx vertx = Vertx.vertx();
                NetClient netClient = vertx.createNetClient();
                CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();

                netClient.connect(selectedServiceMetaInfo.getServicePort(), selectedServiceMetaInfo.getServiceHost(), result -> {
                    if (result.succeeded()) {
                        System.out.println("Connected to server: " + selectedServiceMetaInfo.getServiceAddress());
                        io.vertx.core.net.NetSocket netSocket = result.result();

                        // 使用装饰器模式处理TCP粘包/半包问题
                        TcpPacketDecoder packetDecoder = new TcpPacketDecoder(completeMessages -> {
                            // 处理所有完整的消息
                            for (ProtocolMessage<?> protocolMessageResponse : completeMessages) {
                                // 验证消息类型和消息体类型
                                if (protocolMessageResponse.getHeader().getType() != ProtocolMessageTypeEnum.RESPONSE.getKey()) {
                                    System.err.println("期望响应消息，但收到类型: " + protocolMessageResponse.getHeader().getType());
                                    continue;
                                }

                                if (!(protocolMessageResponse.getBody() instanceof RpcResponse)) {
                                    System.err.println("消息体类型不匹配，期望RpcResponse，实际: " +
                                            (protocolMessageResponse.getBody() != null ? protocolMessageResponse.getBody().getClass().getName() : "null"));
                                    continue;
                                }

                                RpcResponse rpcResponse = (RpcResponse) protocolMessageResponse.getBody();
                                System.out.println(rpcResponse);
                                // 完成Future，将响应返回给调用方
                                responseFuture.complete(rpcResponse);

                                // 收到响应后关闭连接
                                netSocket.close();
                                break; // 只处理第一个有效响应
                            }
                        });

                        // 设置处理器
                        netSocket.handler(packetDecoder);

                        // 发送请求
                        // 构造消息
                        ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
                        ProtocolMessage.Header header = new ProtocolMessage.Header();
                        header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
                        header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
                        header.setSerializer((byte) SerializerType.getKeyByType(RpcApplication.getRpcConfig().getSerializerType()));
                        header.setType((byte) ProtocolMessageTypeEnum.REQUEST.getKey());
                        header.setRequestId(System.currentTimeMillis());
                        header.setBodyLength(bodyBytes.length);
                        protocolMessage.setHeader(header);
                        protocolMessage.setBody(rpcRequest);
                        try {
                            // 按协议编码
                            Buffer encodedBuffer = CompactProtocolCodec.encode(protocolMessage);
                            // 发送请求
                            netSocket.write(encodedBuffer);
                        } catch (IOException e) {
                            e.printStackTrace();
                            responseFuture.completeExceptionally(e);
                        }

                        // 设置超时处理
                        vertx.setTimer(5000, timerId -> {
                            if (!responseFuture.isDone()) {
                                responseFuture.completeExceptionally(new RuntimeException("请求超时"));
                                netSocket.close();
                            }
                        });

                    } else {
                        System.err.println("连接失败: " + result.cause().getMessage());
                        responseFuture.completeExceptionally(result.cause());
                    }
                });

                try {
                    return responseFuture.get();
                } finally {
                    // 关闭连接
                    netClient.close();
                }
            });
            return retryRpcResponse.getData();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}