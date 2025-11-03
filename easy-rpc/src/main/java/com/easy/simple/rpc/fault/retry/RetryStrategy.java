package com.easy.simple.rpc.fault.retry;



import com.easy.simple.rpc.enity.RpcResponse;

import java.util.concurrent.Callable;

/**
 * 重试策略
 */
public interface RetryStrategy {

    /**
     * 重试
     *
     * @param callable 可调用的任务
     * @return 重试后的 RPC 响应
     * @throws Exception 如果重试失败
     */
    RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception;
}
