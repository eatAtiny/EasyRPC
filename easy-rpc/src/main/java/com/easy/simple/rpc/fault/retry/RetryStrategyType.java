package com.easy.simple.rpc.fault.retry;

import lombok.Getter;

public enum RetryStrategyType {
    NO(0, "no", "不重试"),
    FIXED_INTERVAL(1, "fixedInterval", "固定时间间隔");

    @Getter
    final int key;
    @Getter
    final String type;
    @Getter
    final String desc;

    RetryStrategyType(int key, String type, String desc) {
        this.key = key;
        this.type = type;
        this.desc = desc;
    }

    /**
     * 根据类型获取枚举实例
     *
     * @param type 类型
     * @return 枚举实例
     */
    public static RetryStrategyType getByType(String type) {
        for (RetryStrategyType retryStrategyType : values()) {
            if (retryStrategyType.type.equals(type)) {
                return retryStrategyType;
            }
        }
        return null;
    }

    /**
     * 根据key获取type
     *
     * @param key key
     * @return type
     */
    public static String getTypeByKey(int key) {
        for (RetryStrategyType retryStrategyType : values()) {
            if (retryStrategyType.key == key) {
                return retryStrategyType.type;
            }
        }
        return null;
    }
}
