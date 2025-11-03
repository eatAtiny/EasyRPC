package com.easy.simple.rpc.protocol;

import lombok.Getter;

/**
 * 协议消息的类型枚举
 *
 */
@Getter
public enum ProtocolMessageTypeEnum {

    REQUEST(0, "请求"),
    RESPONSE(1, "响应"),
    HEART_BEAT(2, "心跳"),
    OTHERS(3, "其他");

    private final int key;
    private final String desc;

    ProtocolMessageTypeEnum(int key, String desc) {
        this.key = key;
        this.desc = desc;
    }

    /**
     * 根据 key 获取枚举
     *
     * @param key 协议消息类型键值
     * @return 协议消息类型枚举
     */
    public static ProtocolMessageTypeEnum getEnumByKey(int key) {
        for (ProtocolMessageTypeEnum anEnum : ProtocolMessageTypeEnum.values()) {
            if (anEnum.key == key) {
                return anEnum;
            }
        }
        return null;
    }
}
