package com.easy.simple.rpc.protocol;

import com.easy.simple.rpc.serializer.Serializer;
import com.easy.simple.rpc.serializer.SerializerFactory;
import com.easy.simple.rpc.serializer.SerializerType;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 紧凑协议编码解码器
 * 不修改ProtocolMessage结构，只负责压缩请求头和序列化消息体
 * 协议格式：[压缩头部(变长) | 消息体字节数组]
 */
public class CompactProtocolCodec {

    /**
     * 编码协议消息为紧凑字节数组
     * 
     * @param message 协议消息
     * @return 编码后的字节数组
     */
    public static Buffer encode(ProtocolMessage<?> message) throws IOException {
        if (message == null || message.getHeader() == null) {
            throw new IllegalArgumentException("Protocol message or header cannot be null");
        }

        // 1. 序列化消息体
        byte[] bodyBytes = new byte[0];
        if (message.getBody() != null) {
            Serializer serializer = SerializerFactory.getInstance(SerializerType.getTypeByKey(message.getHeader().getSerializer()));
            bodyBytes = serializer.serialize(message.getBody());
        }

        // 2. 更新消息体长度
        message.getHeader().setBodyLength(bodyBytes.length);

        // 3. 压缩请求头
        byte[] headerBytes = compressHeader(message.getHeader());

        // 4. 合并头部和消息体
        ByteBuffer buffer = ByteBuffer.allocate(headerBytes.length + bodyBytes.length);
        buffer.put(headerBytes);
        buffer.put(bodyBytes);

        return Buffer.buffer(buffer.array());
    }

    /**
     * 解码字节数组为协议消息（根据消息类型自动选择消息体类型）
     * 
     * @param buffer 字节数组
     * @return 解码后的协议消息
     */
    public static ProtocolMessage<?> decode(Buffer buffer) throws IOException {
        if (buffer == null || buffer.length() == 0) {
            throw new IllegalArgumentException("Buffer cannot be null or empty");
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer.getBytes());
        
        // 1. 解压缩请求头
        ProtocolMessage.Header header = decompressHeader(byteBuffer);
        
        // 2. 提取消息体字节
        byte[] bodyBytes = new byte[header.getBodyLength()];
        byteBuffer.get(bodyBytes);
        
        // 3. 根据消息类型选择对应的消息体类型
        Object body = null;
        if (bodyBytes.length > 0) {
            Serializer serializer = SerializerFactory.getInstance(SerializerType.getTypeByKey(header.getSerializer()));
            
            // 根据消息类型动态选择消息体类型
            ProtocolMessageTypeEnum messageType = ProtocolMessageTypeEnum.getEnumByKey(header.getType());
            if (messageType != null) {
                switch (messageType) {
                    case REQUEST:
                        body = serializer.deserialize(bodyBytes, com.easy.simple.rpc.enity.RpcRequest.class);
                        break;
                    case RESPONSE:
                        body = serializer.deserialize(bodyBytes, com.easy.simple.rpc.enity.RpcResponse.class);
                        break;
                    case HEART_BEAT:
                        // 心跳消息体可以为空或简单对象
                        if (bodyBytes.length > 0) {
                            body = serializer.deserialize(bodyBytes, Object.class);
                        }
                        break;
                    case OTHERS:
                        // 其他类型消息体，默认使用Object
                        body = serializer.deserialize(bodyBytes, Object.class);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported message type: " + messageType);
                }
            } else {
                // 如果无法识别消息类型，使用Object作为默认类型
                body = serializer.deserialize(bodyBytes, Object.class);
            }
        }
        
        // 4. 构建协议消息
        ProtocolMessage<Object> message = new ProtocolMessage<>();
        message.setHeader(header);
        message.setBody(body);
        
        return message;
    }

    /**
     * 压缩请求头为字节数组（使用变长编码）
     */
    private static byte[] compressHeader(ProtocolMessage.Header header) {
        // 计算变长字段的长度
        int requestIdLength = getVarIntLength(header.getRequestId());
        int bodyLengthLength = getVarIntLength(header.getBodyLength());
        
        // 总长度 = 固定头部(5字节) + 变长请求ID + 变长消息体长度
        int totalLength = 5 + requestIdLength + bodyLengthLength;
        
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        
        // 写入固定头部
        buffer.put(header.getMagic());
        buffer.put(header.getVersion());
        buffer.put(header.getSerializer());
        buffer.put(header.getType());
        buffer.put(header.getStatus());
        
        // 写入变长请求ID
        putVarInt(buffer, header.getRequestId());
        
        // 写入变长消息体长度
        putVarInt(buffer, header.getBodyLength());
        
        return buffer.array();
    }

    /**
     * 解压缩请求头
     */
    private static ProtocolMessage.Header decompressHeader(ByteBuffer buffer) {
        ProtocolMessage.Header header = new ProtocolMessage.Header();
        
        // 读取固定头部
        byte magic = buffer.get();
        byte version = buffer.get();
        
        // 验证魔数
        if (magic != ProtocolConstant.PROTOCOL_MAGIC) {
            throw new IllegalArgumentException("Invalid protocol magic: " + magic + ", expected: " + ProtocolConstant.PROTOCOL_MAGIC);
        }
        
        // 验证版本号
        if (version != ProtocolConstant.PROTOCOL_VERSION) {
            throw new IllegalArgumentException("Unsupported protocol version: " + version + ", expected: " + ProtocolConstant.PROTOCOL_VERSION);
        }
        
        header.setMagic(magic);
        header.setVersion(version);
        header.setSerializer(buffer.get());
        header.setType(buffer.get());
        header.setStatus(buffer.get());
        
        // 读取变长请求ID
        header.setRequestId(getVarInt(buffer));
        
        // 读取变长消息体长度
        header.setBodyLength((int) getVarInt(buffer));
        
        return header;
    }

    /**
     * 写入变长整数（类似Protobuf的varint编码）
     */
    private static void putVarInt(ByteBuffer buffer, long value) {
        while (true) {
            if ((value & ~0x7FL) == 0) {
                buffer.put((byte) value);
                return;
            } else {
                buffer.put((byte) ((value & 0x7F) | 0x80));
                value >>>= 7;
            }
        }
    }

    /**
     * 读取变长整数
     */
    private static long getVarInt(ByteBuffer buffer) {
        long result = 0;
        int shift = 0;
        while (true) {
            byte b = buffer.get();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
            if (shift >= 64) {
                throw new RuntimeException("VarInt too long");
            }
        }
    }

    /**
     * 计算变长整数的字节长度
     */
    private static int getVarIntLength(long value) {
        int length = 0;
        do {
            length++;
            value >>>= 7;
        } while (value != 0);
        return length;
    }

    /**
     * 检查缓冲区是否包含完整的消息
     */
    public static boolean hasCompleteMessage(Buffer buffer) {
        if (buffer == null || buffer.length() < 5) {
            return false;
        }
        
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer.getBytes());
            
            // 跳过固定头部
            byteBuffer.position(5);
            
            // 读取变长请求ID
            long requestId = getVarInt(byteBuffer);
            
            // 读取变长消息体长度
            long bodyLength = getVarInt(byteBuffer);
            
            // 检查是否有足够的字节
            int currentPosition = byteBuffer.position();
            int requiredLength = currentPosition + (int) bodyLength;
            
            return buffer.length() >= requiredLength;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 
     */

    /**
     * 尝试解析消息长度（不抛出异常）
     * 
     * @param buffer 缓冲区
     * @return 消息总长度，如果缓冲区数据不足以解析消息长度则返回-1
     */
    public static int tryParseMessageLength(Buffer buffer) {
        if (buffer == null || buffer.length() < 5) {
            return -1;
        }
        
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer.getBytes());
            
            // 跳过固定头部
            byteBuffer.position(5);
            
            // 读取变长请求ID
            long requestId = getVarInt(byteBuffer);
            
            // 读取变长消息体长度
            long bodyLength = getVarInt(byteBuffer);
            
            // 计算总长度
            return byteBuffer.position() + (int) bodyLength;
            
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 获取消息总长度
     */
    public static int getMessageLength(Buffer buffer) {
        if (buffer == null || buffer.length() < 5) {
            return -1;
        }
        
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer.getBytes());
            
            // 跳过固定头部
            byteBuffer.position(5);
            
            // 读取变长请求ID
            long requestId = getVarInt(byteBuffer);
            
            // 读取变长消息体长度
            long bodyLength = getVarInt(byteBuffer);
            
            // 计算总长度
            return byteBuffer.position() + (int) bodyLength;
            
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 计算编码后的消息大小（用于预估）
     */
    public static int calculateEncodedSize(ProtocolMessage<?> message) throws IOException {
        if (message == null || message.getHeader() == null) {
            return 0;
        }

        // 序列化消息体
        byte[] bodyBytes = new byte[0];
        if (message.getBody() != null) {
            Serializer serializer = SerializerFactory.getInstance(SerializerType.getTypeByKey(message.getHeader().getSerializer()));
            bodyBytes = serializer.serialize(message.getBody());
        }

        // 计算压缩头部大小
        int headerSize = 5 + // 固定头部
                getVarIntLength(message.getHeader().getRequestId()) + // 变长请求ID
                getVarIntLength(bodyBytes.length); // 变长消息体长度

        return headerSize + bodyBytes.length;
    }
}