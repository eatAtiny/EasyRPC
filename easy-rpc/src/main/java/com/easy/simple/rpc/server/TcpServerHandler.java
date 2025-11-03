package com.easy.simple.rpc.server;

import com.easy.simple.rpc.enity.RpcRequest;
import com.easy.simple.rpc.enity.RpcResponse;
import com.easy.simple.rpc.protocol.CompactProtocolCodec;
import com.easy.simple.rpc.protocol.ProtocolMessage;
import com.easy.simple.rpc.protocol.ProtocolMessageTypeEnum;
import com.easy.simple.rpc.protocol.TcpPacketDecoder;
import com.easy.simple.rpc.registry.impl.LocalRegistry;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

public class TcpServerHandler implements Handler<NetSocket> {

    @Override
    public void handle(NetSocket netSocket) {
        // 使用装饰器模式处理TCP粘包/半包问题
        TcpPacketDecoder packetDecoder = new TcpPacketDecoder(completeMessages -> {
            // 处理所有完整的消息
            for (ProtocolMessage<?> protocolMessage : completeMessages) {
                processSingleMessage(protocolMessage, netSocket);
            }
        });
        
        // 设置处理器
        netSocket.handler(packetDecoder);
        
        // 连接关闭时清理资源
        netSocket.closeHandler(v -> {
            System.out.println("连接关闭: " + netSocket.remoteAddress());
        });
        
        // 异常处理
        netSocket.exceptionHandler(e -> {
            System.err.println("连接异常: " + e.getMessage());
            netSocket.close();
        });
    }
    
    private void processSingleMessage(ProtocolMessage<?> protocolMessage, NetSocket netSocket) {
        // 验证消息类型和消息体类型
        if (protocolMessage.getHeader().getType() != ProtocolMessageTypeEnum.REQUEST.getKey()) {
            System.err.println("期望请求消息，但收到类型: " + protocolMessage.getHeader().getType());
            return;
        }
        
        if (!(protocolMessage.getBody() instanceof RpcRequest)) {
            System.err.println("消息体类型不匹配，期望RpcRequest，实际: " + 
                (protocolMessage.getBody() != null ? protocolMessage.getBody().getClass().getName() : "null"));
            return;
        }
        
        RpcRequest rpcRequest = (RpcRequest) protocolMessage.getBody();

        // 处理请求
        // 构造响应结果对象
        RpcResponse rpcResponse = new RpcResponse();
        try {
            // 获取要调用的服务实现类，通过反射调用
            Class<?> implClass = LocalRegistry.get(rpcRequest.getServiceName());
            Method method = implClass.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
            Object result = method.invoke(implClass.getDeclaredConstructor().newInstance(), rpcRequest.getArgs());
            // 封装返回结果
            rpcResponse.setData(result);
            rpcResponse.setDataType(method.getReturnType());
            rpcResponse.setMessage("ok");
        } catch (Exception e) {
            e.printStackTrace();
            rpcResponse.setMessage(e.getMessage());
            rpcResponse.setException(e);
        }

        // 发送响应，编码
        ProtocolMessage.Header header = protocolMessage.getHeader();
        header.setType((byte) ProtocolMessageTypeEnum.RESPONSE.getKey());
        ProtocolMessage<RpcResponse> responseProtocolMessage = new ProtocolMessage<>(header, rpcResponse);
        try {
            Buffer encode = CompactProtocolCodec.encode(responseProtocolMessage);
            netSocket.write(encode);
        } catch (IOException e) {
            System.err.println("协议消息编码错误: " + e.getMessage());
        }
    }
}