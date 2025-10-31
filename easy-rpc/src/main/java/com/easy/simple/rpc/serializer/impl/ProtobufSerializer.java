package com.easy.simple.rpc.serializer.impl;

import com.easy.simple.rpc.serializer.Serializer;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;

import java.io.IOException;
import java.io.Serializable;

public class ProtobufSerializer implements Serializer {
    @Override
    public <T> byte[] serialize(T object) throws IOException {
        if (object == null) {
            return null;
        }
        
        if (object instanceof MessageOrBuilder) {
            // 如果对象是Protobuf消息类型
            if (object instanceof Message.Builder) {
                // 如果是Builder类型，先build再序列化
                return ((Message.Builder) object).build().toByteArray();
            } else if (object instanceof Message) {
                // 如果已经是Message类型，直接序列化
                return ((Message) object).toByteArray();
            }
        } else if (object instanceof Serializable) {
            // 对于普通可序列化对象，使用Java序列化机制
            return SerializationUtils.serialize((Serializable) object);
        }
        throw new IllegalArgumentException("Object must be either a Protobuf Message/Builder or Serializable");
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        if (Message.class.isAssignableFrom(type)) {
            // 如果目标类型是Protobuf消息类型，尝试调用parseFrom方法
            try {
                // 使用反射调用parseFrom方法
                return type.cast(type.getMethod("parseFrom", byte[].class).invoke(null, bytes));
            } catch (Exception e) {
                throw new IOException("Failed to parse Protobuf message", e);
            }
        } else if (Serializable.class.isAssignableFrom(type)) {
            // 对于普通可序列化对象，从字节数组中反序列化
            try {
                return type.cast(SerializationUtils.deserialize(bytes));
            } catch (ClassNotFoundException e) {
                throw new IOException("Failed to deserialize object", e);
            }
        }
        throw new IllegalArgumentException("Type must be either a Protobuf Message or Serializable");
    }

    // 简单的序列化工具类，用于处理普通Java对象
    private static class SerializationUtils {
        public static byte[] serialize(Serializable obj) throws IOException {
            if (obj == null) {
                return null;
            }
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();
            return baos.toByteArray();
        }

        public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
            if (data == null || data.length == 0) {
                return null;
            }
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
            java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais);
            Object obj = ois.readObject();
            ois.close();
            return obj;
        }
    }
}