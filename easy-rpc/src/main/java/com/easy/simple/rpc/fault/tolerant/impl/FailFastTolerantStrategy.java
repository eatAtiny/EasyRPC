package com.easy.simple.rpc.fault.tolerant.impl;

import com.easy.simple.rpc.enity.RpcResponse;
import com.easy.simple.rpc.fault.tolerant.TolerantStrategy;

import java.util.Map;

/**
 * 快速失败 - 容错策略（立刻通知外层调用方）
 */
public class FailFastTolerantStrategy implements TolerantStrategy {

    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        throw new RuntimeException("服务报错", e);
    }
}
