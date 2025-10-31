package com.easy.simple.rpc.serializer.impl;

import com.easy.simple.rpc.enity.RpcRequest;
import com.easy.simple.rpc.enity.RpcResponse;
import com.easy.simple.rpc.serializer.Serializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

/**
 * 增强版JSON序列化器
 * 解决类型信息丢失问题
 */
public class JsonSerializer implements Serializer {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeFactory TYPE_FACTORY = OBJECT_MAPPER.getTypeFactory();

    @Override
    public <T> byte[] serialize(T object) throws IOException {
        if (object instanceof RpcRequest) {
            // 对RpcRequest进行特殊处理，确保类型信息被正确序列化
            return serializeRpcRequest((RpcRequest) object);
        } else if (object instanceof RpcResponse) {
            // 对RpcResponse进行特殊处理，确保类型信息被正确序列化
            return serializeRpcResponse((RpcResponse) object);
        }
        return OBJECT_MAPPER.writeValueAsBytes(object);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) throws IOException {
        if (type == RpcRequest.class) {
            // 对RpcRequest进行特殊处理，确保类型信息被正确反序列化
            return type.cast(deserializeRpcRequest(bytes));
        } else if (type == RpcResponse.class) {
            // 对RpcResponse进行特殊处理，确保类型信息被正确反序列化
            return type.cast(deserializeRpcResponse(bytes));
        }
        return OBJECT_MAPPER.readValue(bytes, type);
    }

    /**
     * 序列化RpcRequest，特殊处理参数类型和参数值
     */
    private byte[] serializeRpcRequest(RpcRequest request) throws IOException {
        // 创建一个可序列化的Map来存储请求信息，包括类型信息
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("serviceName", request.getServiceName());
        requestMap.put("methodName", request.getMethodName());
        
        // 处理参数类型
        if (request.getParameterTypes() != null) {
            String[] typeNames = new String[request.getParameterTypes().length];
            for (int i = 0; i < request.getParameterTypes().length; i++) {
                typeNames[i] = request.getParameterTypes()[i].getName();
            }
            requestMap.put("parameterTypes", typeNames);
        }
        
        // 参数值直接放入Map
        requestMap.put("args", request.getArgs());
        
        return OBJECT_MAPPER.writeValueAsBytes(requestMap);
    }

    /**
     * 反序列化RpcRequest，重建参数类型和参数值
     */
    private RpcRequest deserializeRpcRequest(byte[] bytes) throws IOException {
        // 先反序列化为Map获取原始数据
        Map<String, Object> requestMap = OBJECT_MAPPER.readValue(bytes, HashMap.class);
        
        RpcRequest request = new RpcRequest();
        request.setServiceName((String) requestMap.get("serviceName"));
        request.setMethodName((String) requestMap.get("methodName"));
        
        // 重建参数类型 - 修复类型转换问题
        Object parameterTypesObj = requestMap.get("parameterTypes");
        if (parameterTypesObj != null) {
            String[] typeNames;
            // 检查parameterTypesObj是否为ArrayList类型，如果是则转换为String数组
            if (parameterTypesObj instanceof java.util.ArrayList) {
                java.util.ArrayList<?> list = (java.util.ArrayList<?>) parameterTypesObj;
                typeNames = list.toArray(new String[0]);
            } else if (parameterTypesObj instanceof String[]) {
                // 如果已经是String数组，直接使用
                typeNames = (String[]) parameterTypesObj;
            } else {
                // 其他情况，尝试转换为String
                typeNames = new String[]{parameterTypesObj.toString()};
            }
            
            Class<?>[] parameterTypes = new Class<?>[typeNames.length];
            for (int i = 0; i < typeNames.length; i++) {
                try {
                    parameterTypes[i] = Class.forName(typeNames[i]);
                } catch (ClassNotFoundException e) {
                    throw new IOException("Failed to load class: " + typeNames[i], e);
                }
            }
            request.setParameterTypes(parameterTypes);
        }
        
        // 重建参数值
        Object[] args = null;
        Object argsObj = requestMap.get("args");
        if (argsObj != null) {
            // 处理args可能是ArrayList的情况
            if (argsObj instanceof java.util.ArrayList) {
                java.util.ArrayList<?> list = (java.util.ArrayList<?>) argsObj;
                args = list.toArray();
            } else if (argsObj.getClass().isArray()) {
                // 如果已经是数组，转换为Object[]
                int length = Array.getLength(argsObj);
                args = new Object[length];
                for (int i = 0; i < length; i++) {
                    args[i] = Array.get(argsObj, i);
                }
            } else {
                // 单个参数的情况
                args = new Object[]{argsObj};
            }
        }
        
        if (args != null && request.getParameterTypes() != null && args.length == request.getParameterTypes().length) {
            // 对参数进行类型转换
            Object[] convertedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                convertedArgs[i] = convertArgument(args[i], request.getParameterTypes()[i]);
            }
            request.setArgs(convertedArgs);
        } else {
            request.setArgs(args);
        }
        
        return request;
    }

    /**
     * 将反序列化的参数转换为目标类型
     */
    private Object convertArgument(Object arg, Class<?> targetType) {
        // 如果参数已经是目标类型，直接返回
        if (arg == null || targetType.isInstance(arg)) {
            return arg;
        }
        
        // 处理Map转对象的情况（这是JSON反序列化常见的问题）
        if (arg instanceof Map && !targetType.isPrimitive() && !targetType.getName().startsWith("java.lang.")) {
            try {
                // 将Map转换回原始对象类型
                return OBJECT_MAPPER.convertValue(arg, targetType);
            } catch (Exception e) {
                // 转换失败，返回原始对象
                return arg;
            }
        }
        
        // 处理基本类型转换
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(arg.toString());
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(arg.toString());
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(arg.toString());
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(arg.toString());
        } else if (targetType == String.class) {
            return arg.toString();
        }
        
        // 处理数组类型
        if (targetType.isArray()) {
            Class<?> componentType = targetType.getComponentType();
            if (arg instanceof Iterable) {
                // 转换集合为数组
                Iterable<?> iterable = (Iterable<?>) arg;
                java.util.List<Object> list = new java.util.ArrayList<>();
                iterable.forEach(list::add);
                Object[] array = (Object[]) Array.newInstance(componentType, list.size());
                for (int i = 0; i < list.size(); i++) {
                    array[i] = convertArgument(list.get(i), componentType);
                }
                return array;
            }
        }
        
        // 其他情况返回原始对象
        return arg;
    }

    /**
     * 序列化RpcResponse，确保dataType信息被正确序列化
     */
    private byte[] serializeRpcResponse(RpcResponse response) throws IOException {
        // 创建一个可序列化的Map来存储响应信息
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("message", response.getMessage());
        responseMap.put("exception", response.getException());
        
        // 存储数据和数据类型
        Object data = response.getData();
        responseMap.put("data", data);
        
        // 如果data不为null且dataType为null，尝试自动设置dataType
        if (data != null && response.getDataType() == null) {
            responseMap.put("dataType", data.getClass().getName());
        } else if (response.getDataType() != null) {
            responseMap.put("dataType", response.getDataType().getName());
        }
        
        return OBJECT_MAPPER.writeValueAsBytes(responseMap);
    }

    /**
     * 反序列化RpcResponse，确保data被正确转换为指定类型
     */
    private RpcResponse deserializeRpcResponse(byte[] bytes) throws IOException {
        // 先反序列化为Map获取原始数据
        Map<String, Object> responseMap = OBJECT_MAPPER.readValue(bytes, HashMap.class);
        
        RpcResponse response = new RpcResponse();
        response.setMessage((String) responseMap.get("message"));
        response.setException((Exception) responseMap.get("exception"));
        
        // 获取数据类型
        Object dataTypeObj = responseMap.get("dataType");
        Class<?> dataType = null;
        if (dataTypeObj != null) {
            try {
                dataType = Class.forName(dataTypeObj.toString());
                response.setDataType(dataType);
            } catch (ClassNotFoundException e) {
                // 如果无法加载类，记录警告但继续处理
                System.err.println("Warning: Failed to load data type: " + dataTypeObj);
            }
        }
        
        // 获取数据并尝试进行类型转换
        Object data = responseMap.get("data");
        if (data != null && dataType != null) {
            // 使用convertArgument方法进行类型转换
            data = convertArgument(data, dataType);
        }
        response.setData(data);
        
        return response;
    }
}