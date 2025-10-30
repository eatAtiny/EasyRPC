package com.easy.simple.rpc.serializer;

import com.easy.simple.rpc.enity.RpcRequest;
import com.easy.simple.rpc.enity.RpcResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * JSON 序列化器
 */
public class JSONSerializer implements Serializer {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public <T> byte[] serialize(T object) throws IOException {
        return OBJECT_MAPPER.writeValueAsBytes(object);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) throws IOException {
        return OBJECT_MAPPER.readValue(bytes, type);
    }
}
