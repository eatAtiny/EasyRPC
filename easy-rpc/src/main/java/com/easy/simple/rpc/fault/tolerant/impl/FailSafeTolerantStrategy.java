package com.easy.simple.rpc.fault.tolerant.impl;
import com.easy.simple.rpc.enity.RpcResponse;
import com.easy.simple.rpc.fault.tolerant.TolerantStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 静默处理异常 - 容错策略
 */
@Slf4j
public class FailSafeTolerantStrategy implements TolerantStrategy {

    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        log.info("静默处理异常", e);
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setMessage("服务调用失败，静默处理异常");
        return rpcResponse;
    }
}
