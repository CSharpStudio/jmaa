package org.jmaa.sdk.https;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求处理器声明
 *
 * @author Eric Liang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestHandler {
    AuthType auth();

    HandlerType type();

    enum HandlerType {
        /** application/json */
        JSON,
        /** text/html */
        HTTP,
        /** 自定义 */
        // RAW,
    }

    enum AuthType {
        /** 匿名访问 */
        NONE,
        /** 授权用户访问 */
        USER,
    }
}
