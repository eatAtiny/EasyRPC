package com.easy.simple.rpc.server.impl;

import com.easy.simple.rpc.server.WebServer;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;

/**
 * Vertx TCP 服务器
 */
public class VertxTcpServer implements WebServer {

    @Override
    public void doStart(int port) {
        // 创建 Vert.x 实例
        Vertx vertx = Vertx.vertx();

        // 创建 TCP 服务器
        NetServer server = vertx.createNetServer();

        // 处理请求
        server.connectHandler(new TcpServerHandler());

        // 启动 TCP 服务器并监听指定端口
        server.listen(port, result -> {
            if (result.succeeded()) {
                System.out.println("TCP server started on port " + port);
            } else {
                System.err.println("Failed to start TCP server: " + result.cause());
            }
        });
    }
}