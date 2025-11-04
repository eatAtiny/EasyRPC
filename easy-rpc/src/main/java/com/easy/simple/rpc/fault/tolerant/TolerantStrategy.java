package com.easy.simple.rpc.fault.tolerant;

import com.easy.simple.rpc.enity.RpcResponse;

import java.util.Map;

/**
 * 容错策略
 */
public interface TolerantStrategy {

    /**
     * 容错
     *
     * @param context 上下文，用于传递数据
     * @param e       异常
     * @return 恢复后的RpcResponse
     */
    RpcResponse doTolerant(Map<String, Object> context, Exception e);
}
