package com.easy.simple.rpc.annotation;

import com.easy.simple.rpc.bootstrap.RpcConsumerBootstrap;
import com.easy.simple.rpc.bootstrap.RpcInitBootstrap;
import com.easy.simple.rpc.bootstrap.RpcProviderBootstrap;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 RPC 框架
 * 自动配置 RPC 相关组件
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({RpcInitBootstrap.class, RpcConsumerBootstrap.class, RpcProviderBootstrap.class})
public @interface EnableRpc {

    /**
     * 是否需要启动服务器（服务提供者模式）
     * 默认启动服务器，设置为 false 则仅作为消费者模式运行
     *
     * @return 是否启动服务器
     */
    boolean needServer() default true;
}