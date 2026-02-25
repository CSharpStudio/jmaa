package org.jmaa.server;

import org.jmaa.sdk.tenants.TenantService;
import org.jmaa.sdk.tools.SpringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * web应用
 *
 * @author Eric Liang
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages = {"org.jmaa"})
@EnableDiscoveryClient
public class WebApplication {
    public static void main(String[] args) {
        SpringUtils.setApplicationContext(SpringApplication.run(WebApplication.class, args));
        TenantService.startup();
    }
}
