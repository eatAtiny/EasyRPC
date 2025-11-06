package com.easy.simple.rpc.server;

/**
 * 服务器接口（统一HTTP和TCP服务器）
 */
public interface WebServer {

    /**
     * 启动服务器
     *
     * @param port 端口号
     */
    void doStart(int port);
}