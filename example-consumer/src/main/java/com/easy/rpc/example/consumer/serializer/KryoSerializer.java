package com.easy.rpc.example.consumer.serializer;

import com.easy.simple.rpc.serializer.Serializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Kryo序列化器实现
 * Kryo是一种高性能的Java序列化框架
 */
public class KryoSerializer implements Serializer {

    // 使用ThreadLocal存储Kryo实例，避免多线程问题
    private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        // 设置是否需要注册类（默认为true）
        kryo.setRegistrationRequired(false);
        // 设置引用跟踪（避免循环引用问题）
        kryo.setReferences(true);
        return kryo;
    });

    @Override
    public <T> byte[] serialize(T object) throws IOException {
        System.out.println("custom kryo serialize");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);
        Kryo kryo = KRYO_THREAD_LOCAL.get();
        try {
            kryo.writeObject(output, object);
            output.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new IOException("Kryo serialize error", e);
        } finally {
            output.close();
            // 清理ThreadLocal，防止内存泄漏
            KRYO_THREAD_LOCAL.remove();
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Input input = new Input(byteArrayInputStream);
        Kryo kryo = KRYO_THREAD_LOCAL.get();
        try {
            return kryo.readObject(input, type);
        } catch (Exception e) {
            throw new IOException("Kryo deserialize error", e);
        } finally {
            input.close();
            // 清理ThreadLocal，防止内存泄漏
            KRYO_THREAD_LOCAL.remove();
        }
    }
}