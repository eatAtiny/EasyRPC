package com.easy.simple.rpc.fault.retry;

import com.easy.simple.rpc.fault.retry.impl.NoRetryStrategy;
import com.easy.simple.rpc.utils.SpiLoader;

/**
 * 重试策略工厂（用于获取重试器对象）
 */
public class RetryStrategyFactory {

    static {
        SpiLoader.load(RetryStrategy.class);
    }

    /**
     * 默认重试器
     */
    private static final RetryStrategy DEFAULT_RETRY_STRATEGY = new NoRetryStrategy();

    /**
     * 获取实例
     *
     * @param type 重试策略类型
     * @return 重试策略实例
     */
    public static RetryStrategy getInstance(String type) {
        return SpiLoader.getInstance(RetryStrategy.class, type);
    }

}
