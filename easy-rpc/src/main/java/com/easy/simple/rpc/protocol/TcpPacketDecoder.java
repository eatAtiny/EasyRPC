package com.easy.simple.rpc.protocol;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;

import java.util.ArrayList;
import java.util.List;

/**
 * TCP粘包/半包解码器（使用RecordParser实现）
 * 对Handler<Buffer>进行增强，自动处理TCP流式数据的粘包和半包问题
 */
public class TcpPacketDecoder implements Handler<Buffer> {
    
    private final Handler<List<ProtocolMessage<?>>> decoratedHandler;
    private final RecordParser recordParser;
    
    /**
     * 构造函数
     * 
     * @param decoratedHandler 被装饰的处理器，接收解析后的完整协议消息列表
     */
    public TcpPacketDecoder(Handler<List<ProtocolMessage<?>>> decoratedHandler) {
        this.decoratedHandler = decoratedHandler;
        this.recordParser = initRecordParser();
    }
    
    @Override
    public void handle(Buffer buffer) {
        recordParser.handle(buffer);
    }
    
    private RecordParser initRecordParser() {
        // 初始化为固定长度模式，用于读取固定头部（5字节）
        RecordParser parser = RecordParser.newFixed(5);
        
        parser.setOutput(new Handler<Buffer>() {
            // 当前消息长度
            int messageLength = -1;
            // 累积的完整消息缓冲区
            Buffer resultBuffer = Buffer.buffer();
            // 当前解析阶段
            ParseStage currentStage = ParseStage.FIXED_HEADER;
            
            @Override
            public void handle(Buffer buffer) {
                switch (currentStage) {
                    case FIXED_HEADER:
                        // 读取固定头部（5字节）
                        resultBuffer.appendBuffer(buffer);
                        
                        // 切换到变长头部解析阶段
                        currentStage = ParseStage.VARIABLE_HEADER;
                        // 使用动态模式，每次读取1字节，直到能够解析出完整消息长度
                        parser.fixedSizeMode(1);
                        break;
                        
                    case VARIABLE_HEADER:
                        // 将新数据添加到缓冲区
                        resultBuffer.appendBuffer(buffer);
                        
                        // 尝试解析完整的消息头长度
                        messageLength = CompactProtocolCodec.tryParseMessageLength(resultBuffer);
                        
                        if (messageLength == -1) {
                            // 消息头还不完整，继续等待数据（每次读取1字节）
                            parser.fixedSizeMode(1);
                            return;
                        }
                        
                        // 计算消息体长度
                        int bodyLength = messageLength - resultBuffer.length();
                        if (bodyLength < 0) {
                            // 数据异常，重置解析器
                            resetParser(parser);
                            return;
                        }
                        
                        // 切换到消息体解析阶段
                        currentStage = ParseStage.BODY;
                        parser.fixedSizeMode(bodyLength);
                        break;
                        
                    case BODY:
                        // 读取消息体
                        resultBuffer.appendBuffer(buffer);
                        
                        try {
                            // 解码完整消息
                            ProtocolMessage<?> protocolMessage = CompactProtocolCodec.decode(resultBuffer);
                            List<ProtocolMessage<?>> messages = new ArrayList<>();
                            messages.add(protocolMessage);
                            
                            // 调用被装饰的处理器
                            decoratedHandler.handle(messages);
                        } catch (Exception e) {
                            System.err.println("协议消息解码失败: " + e.getMessage());
                        }
                        
                        // 重置解析器状态，准备读取下一个消息
                        resetParser(parser);
                        break;
                }
            }
            
            private void resetParser(RecordParser parser) {
                currentStage = ParseStage.FIXED_HEADER;
                messageLength = -1;
                resultBuffer = Buffer.buffer();
                parser.fixedSizeMode(5);
            }
        });
        
        return parser;
    }
    
    /**
     * 解析阶段枚举
     */
    private enum ParseStage {
        FIXED_HEADER,    // 固定头部解析阶段（5字节）
        VARIABLE_HEADER, // 变长头部解析阶段（requestId + bodyLength）
        BODY             // 消息体解析阶段
    }
}