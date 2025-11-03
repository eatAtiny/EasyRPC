package com.easy.simple.rpc.protocol;

import lombok.Getter;

/**
 * 协议消息的状态枚举
 *
 */
@Getter
public enum ProtocolMessageStatusEnum {

    OK("ok", 20, "成功"),
    BAD_REQUEST("badRequest", 40, "请求错误"),
    BAD_RESPONSE("badResponse", 50, "响应错误");

    private final String text;
    private final int value;
    private final String desc;

    ProtocolMessageStatusEnum(String text, int value, String desc) {
        this.text = text;
        this.value = value;
        this.desc = desc;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static ProtocolMessageStatusEnum getEnumByValue(int value) {
        for (ProtocolMessageStatusEnum anEnum : ProtocolMessageStatusEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }
}
