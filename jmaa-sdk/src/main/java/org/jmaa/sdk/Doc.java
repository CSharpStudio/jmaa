package org.jmaa.sdk;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 帮助文档
 *
 * @author Eric Liang
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Doc {
    /**
     * 说明
     *
     * @return
     */
    String doc() default "";

    /**
     * 示例值
     *
     * @return
     */
    String value() default "";

    /**
     * 参数类型
     *
     * @return
     */
    String type() default "";

    /**
     * 示例值
     * @return
     */
    String sample() default "";
}
