package com.easy.simple.rpc.proxy;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.easy.simple.rpc.RpcApplication;
import com.easy.simple.rpc.config.RpcConfig;
import com.easy.simple.rpc.enity.RpcRequest;
import com.easy.simple.rpc.enity.RpcResponse;
import com.easy.simple.rpc.enity.ServiceMetaInfo;
import com.easy.simple.rpc.protocol.CompactProtocolCodec;
import com.easy.simple.rpc.protocol.ProtocolConstant;
import com.easy.simple.rpc.protocol.ProtocolMessage;
import com.easy.simple.rpc.protocol.ProtocolMessageTypeEnum;
import com.easy.simple.rpc.protocol.TcpPacketDecoder;
import com.easy.simple.rpc.serializer.Serializer;
import com.easy.simple.rpc.serializer.SerializerType;
import com.easy.simple.rpc.server.ServerType;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 请求发送器 - 根据配置发送HTTP或TCP请求
 */
public class RequestSender {

    /**
     * 发送RPC请求
     *
     * @param rpcRequest 请求对象
     * @param selectedServiceMetaInfo 选中的服务元信息
     * @param serializer 序列化器
     * @return RPC响应
     */
    public static RpcResponse sendRequest(RpcRequest rpcRequest, ServiceMetaInfo selectedServiceMetaInfo, Serializer serializer) throws Exception {
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        String serverType = rpcConfig.getServerType();
        
        if (serverType.equals(ServerType.HTTP.getType())) {
            return sendHttpRequest(rpcRequest, selectedServiceMetaInfo, serializer);
        } else {
            // 默认使用TCP
            return sendTcpRequest(rpcRequest, selectedServiceMetaInfo, serializer);
        }
    }

    /**
     * 发送HTTP请求
     */
    private static RpcResponse sendHttpRequest(RpcRequest rpcRequest, ServiceMetaInfo selectedServiceMetaInfo, Serializer serializer) throws Exception {
        // 序列化请求
        byte[] bodyBytes = serializer.serialize(rpcRequest);
        
        // 发送HTTP POST请求
        String url = String.format("http://%s:%d", 
            selectedServiceMetaInfo.getServiceHost(), 
            selectedServiceMetaInfo.getServicePort());
        
        // 设置超时时间（连接超时和读取超时都设置为5秒）
        try (HttpResponse httpResponse = HttpRequest.post(url)
                .body(bodyBytes)
                .setConnectionTimeout(RpcApplication.getRpcConfig().getConnectionTimeout())  // 连接超时
                .setReadTimeout(5000)       // 读取超时
                .execute()) {
            
            byte[] result = httpResponse.bodyBytes();
            // 反序列化
            return serializer.deserialize(result, RpcResponse.class);
        }
    }

    /**
     * 发送TCP请求
     */
    private static RpcResponse sendTcpRequest(RpcRequest rpcRequest, ServiceMetaInfo selectedServiceMetaInfo, Serializer serializer) throws Exception {
        Vertx vertx = Vertx.vertx();
        NetClient netClient = vertx.createNetClient();
        CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();

        // 序列化请求体
        byte[] bodyBytes = serializer.serialize(rpcRequest);
        
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
    }
}