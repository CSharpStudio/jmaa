package org.jmaa.sdk.https;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 服务器信息
 */
@Component
public class ServerInfo {
    @Value("${server.port}")
    private int serverPort;

    /**
     * 获取服务端口，从配置 server.port 读取
     *
     * @return
     */
    public int getServerPort() {
        return serverPort;
    }

    @Value("${spring.cloud.client.ip-address:}")
    private String serverAddress;

    /**
     * 获取服务地址，从配置 spring.cloud.client.ip-address 读取
     *
     * @return
     */
    public String getServerAddress() {
        return serverAddress;
    }
}
