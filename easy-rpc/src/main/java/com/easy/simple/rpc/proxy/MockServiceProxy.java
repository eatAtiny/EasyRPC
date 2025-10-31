package com.easy.simple.rpc.proxy;

import com.easy.simple.rpc.RpcApplication;
import com.easy.simple.rpc.enity.RpcRequest;
import com.easy.simple.rpc.serializer.Serializer;
import com.easy.simple.rpc.serializer.SerializerFactory;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.util.*;

/**
 * Mock 服务代理（JDK 动态代理）
 */
@Slf4j
public class MockServiceProxy implements InvocationHandler {

    /**
     * 调用代理
     *
     * @return 模拟的默认值对象
     * @throws Throwable 模拟调用过程中可能抛出的异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 根据方法的返回值类型，生成特定的默认值对象
        Class<?> methodReturnType = method.getReturnType();
        log.info("mock invoke {}", method.getName());
        return getDefaultObject(methodReturnType);
    }

    /**
     * 生成指定类型的默认值对象
     *
     * @param type 方法返回值类型
     * @return 模拟的默认值对象
     */
    private Object getDefaultObject(Class<?> type) {
        // 基本类型
        if (type.isPrimitive()) {
            if (type == boolean.class) {
                return new Random().nextBoolean();
            } else if (type == short.class) {
                return new Random().nextInt(Short.MAX_VALUE);
            } else if (type == int.class) {
                return new Random().nextInt(1000);
            } else if (type == long.class) {
                return new Random().nextLong();
            } else if (type == float.class) {
                return new Random().nextFloat();
            } else if (type == double.class) {
                return new Random().nextDouble();
            } else if (type == char.class) {
                return (char) new Random().nextInt(Character.MAX_VALUE);
            }
        }
        // 常见包装类型和String
        if (type == String.class) {
            return "mock_string_" + new Random().nextInt(1000);
        } else if (type == Integer.class) {
            return new Random().nextInt(1000);
        } else if (type == Long.class) {
            return new Random().nextLong();
        } else if (type == Boolean.class) {
            return new Random().nextBoolean();
        }
        // 集合类型
        if (Collection.class.isAssignableFrom(type)) {
            return generateCollection(type);
        }

        // Map类型
        if (Map.class.isAssignableFrom(type)) {
            return generateMap(type);
        }

        // 数组类型
        if (type.isArray()) {
            return generateArray(type);
        }

        // 枚举类型
        if (type.isEnum()) {
            Object[] enumConstants = type.getEnumConstants();
            return enumConstants != null && enumConstants.length > 0 ?
                    enumConstants[new Random().nextInt(enumConstants.length)] : null;
        }

        // 对象类型 - 使用反射创建对象并设置随机值
        return generateObject(type);
    }

    /**
     * 使用反射生成对象并设置随机属性值
     */
    private Object generateObject(Class<?> type) {
        // 处理接口和抽象类
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return null;
        }

        try {
            // 尝试创建实例
            Object instance = createInstance(type);
            if (instance == null) {
                return null;
            }

            // 防止循环引用的简单实现
            Set<Object> processedObjects = new HashSet<>();
            processedObjects.add(instance);

            // 设置字段值
            setFieldValues(instance, type, processedObjects);

            return instance;
        } catch (Exception e) {
            log.warn("Failed to generate mock object for type {}", type.getName(), e);
            return null;
        }
    }

    /**
     * 创建对象实例
     */
    private Object createInstance(Class<?> type) throws Exception {
        // 尝试使用无参构造函数
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            // 如果没有无参构造函数，尝试使用有参构造函数
            Constructor<?>[] constructors = type.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                constructor.setAccessible(true);
                Class<?>[] paramTypes = constructor.getParameterTypes();
                Object[] params = new Object[paramTypes.length];
                for (int i = 0; i < paramTypes.length; i++) {
                    params[i] = getDefaultObject(paramTypes[i]);
                }
                return constructor.newInstance(params);
            }
        }
        return null;
    }

    /**
     * 设置对象的字段值
     */
    private void setFieldValues(Object instance, Class<?> type, Set<Object> processedObjects) {
        // 获取所有字段，包括父类的字段
        List<Field> allFields = new ArrayList<>();
        Class<?> currentClass = type;
        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            allFields.addAll(Arrays.asList(fields));
            currentClass = currentClass.getSuperclass();
        }

        // 设置字段值
        for (Field field : allFields) {
            // 跳过静态字段和final字段
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }

            try {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();

                // 生成字段值
                Object fieldValue = getDefaultObject(fieldType);

                // 避免循环引用导致的栈溢出
                if (fieldValue != null && !fieldType.isPrimitive() &&
                        !fieldType.getName().startsWith("java.") &&
                        processedObjects.contains(fieldValue)) {
                    continue;
                }

                if (fieldValue != null) {
                    field.set(instance, fieldValue);
                    // 将非基本类型对象添加到已处理集合中
                    if (!fieldType.isPrimitive() && !fieldType.getName().startsWith("java.")) {
                        processedObjects.add(fieldValue);
                    }
                }
            } catch (Exception e) {
                // 如果设置字段值失败，忽略该字段
                log.debug("Failed to set field {} of type {}", field.getName(), type.getName(), e);
            }
        }
    }

    /**
     * 生成集合类型的模拟数据
     */
    private Collection<?> generateCollection(Class<?> collectionType) {
        Collection<Object> collection;

        // 创建集合实例
        try {
            collection = (Collection<Object>) collectionType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // 如果无法实例化，使用ArrayList作为默认实现
            collection = new ArrayList<>();
        }

        // 生成随机数量的元素
        int size = new Random().nextInt(5) + 1; // 1-5个元素

        // 由于泛型擦除，无法直接获取元素类型，这里简单地生成一些常见类型
        for (int i = 0; i < size; i++) {
            collection.add(generateRandomCommonType());
        }

        return collection;
    }

    /**
     * 生成Map类型的模拟数据
     */
    private Map<?, ?> generateMap(Class<?> mapType) {
        Map<Object, Object> map;

        // 创建Map实例
        try {
            map = (Map<Object, Object>) mapType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // 如果无法实例化，使用HashMap作为默认实现
            map = new HashMap<>();
        }

        // 生成随机数量的键值对
        int size = new Random().nextInt(5) + 1; // 1-5个键值对

        for (int i = 0; i < size; i++) {
            // 使用String作为键，随机类型作为值
            map.put("key_" + i, generateRandomCommonType());
        }

        return map;
    }

    /**
     * 生成数组类型的模拟数据
     */
    private Object generateArray(Class<?> arrayType) {
        Class<?> componentType = arrayType.getComponentType();
        int length = new Random().nextInt(5) + 1; // 1-5个元素

        // 创建数组
        Object array = Array.newInstance(componentType, length);

        // 填充数组
        for (int i = 0; i < length; i++) {
            Array.set(array, i, getDefaultObject(componentType));
        }

        return array;
    }

    /**
     * 生成随机的常见类型数据
     */
    private Object generateRandomCommonType() {
        Random random = new Random();
        int typeIndex = random.nextInt(5);
        switch (typeIndex) {
            case 0: return "string_" + random.nextInt(1000);
            case 1: return random.nextInt(1000);
            case 2: return random.nextDouble() * 1000;
            case 3: return random.nextBoolean();
            case 4: return new Date();
            default: return "string_" + random.nextInt(1000);
        }
    }
}