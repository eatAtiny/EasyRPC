package com.easy.simple.rpc.fault.tolerant;

import lombok.Getter;

public enum TolerantStrategyType {
    FAIL_OVER(0, "failOver", "转移到其他服务节点"),
    FAIL_BACK(1, "failBack", "降级到其他服务"),
    FAIL_FAST(2, "failFast", "快速失败"),
    FAIL_SAFE(3, "failSafe", "失败安全");



    @Getter
    final int key;
    @Getter
    final String type;
    @Getter
    final String desc;

    TolerantStrategyType(int key, String type, String desc) {
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
    public static TolerantStrategyType getByType(String type) {
        for (TolerantStrategyType tolerantStrategyType : values()) {
            if (tolerantStrategyType.type.equals(type)) {
                return tolerantStrategyType;
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
        for (TolerantStrategyType tolerantStrategyType : values()) {
            if (tolerantStrategyType.key == key) {
                return tolerantStrategyType.type;
            }
        }
        return null;
    }
}
