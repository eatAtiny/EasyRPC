package com.easy.simple.rpc.fault.retry.impl;

import com.easy.simple.rpc.enity.RpcResponse;
import com.easy.simple.rpc.fault.retry.RetryStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * 不重试 - 重试策略
 */
@Slf4j
public class NoRetryStrategy implements RetryStrategy {

    /**
     * 重试
     *
     * @param callable 可调用的任务
     * @return 重试后的 RPC 响应
     * @throws Exception 如果重试失败
     */
    public RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception {
        return callable.call();
    }

}
