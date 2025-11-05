package com.easy.simple.rpc.annotation;


import com.easy.simple.rpc.constant.RpcConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务消费者注解（用于注入服务）
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RpcReference {

    /**
     * 服务接口类
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 版本
     */
    String serviceVersion() default RpcConstant.DEFAULT_SERVICE_VERSION;

    /**
     * 负载均衡器
     */
    String loadBalancer() default RpcConstant.DEFAULT_LOAD_BALANCER;

    /**
     * 重试策略
     */
    String retryStrategy() default RpcConstant.DEFAULT_RETRY_STRATEGY;

    /**
     * 容错策略
     */
    String tolerantStrategy() default RpcConstant.DEFAULT_TOLERANT_STRATEGY;

    /**
     * 模拟调用
     */
    boolean mock() default false;

}