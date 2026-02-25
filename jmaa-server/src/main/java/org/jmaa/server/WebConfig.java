package org.jmaa.server;

import org.jmaa.base.controllers.ControllerLogInterceptor;
import org.jmaa.sdk.core.Loader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.jmaa.sdk.https.ControllerInterceptor;

/**
 * web配置
 *
 * @author Eric Liang
 */
@EnableWebMvc
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    ControllerInterceptor controllerInterceptor() {
        return Loader.get(ControllerInterceptor.class);
    }
    @Bean
    ControllerLogInterceptor controllerLogInterceptor() {
        return new ControllerLogInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(controllerLogInterceptor());
        registry.addInterceptor(controllerInterceptor());
    }
}
