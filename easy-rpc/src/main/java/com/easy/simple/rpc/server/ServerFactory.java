package com.easy.simple.rpc.server;

import com.easy.simple.rpc.server.impl.VertxTcpServer;
import com.easy.simple.rpc.utils.SpiLoader;

/**
 * 服务器工厂（用于获取服务器对象）
 */
public class ServerFactory {

    static {
        SpiLoader.load(WebServer.class);
    }

    /**
     * 默认服务器
     */
    private static final WebServer DEFAULT_SERVER = new VertxTcpServer();

    /**
     * 获取服务器实例
     *
     * @param serverType 服务器类型
     * @return 服务器实例
     */
    public static WebServer getInstance(String serverType) {
        try{
            return SpiLoader.getInstance(WebServer.class, serverType);
        } catch (Exception e) {
            System.err.println("获取服务器失败: " + e.getMessage());
            System.out.println("使用默认服务器: " + DEFAULT_SERVER.getClass().getName());
            return DEFAULT_SERVER;
        }
    }
}