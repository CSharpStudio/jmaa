package org.jmaa.sdk.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import org.jmaa.sdk.Model;

/**
 * @author Eric Liang
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelServices {
    /**
     * 值
     *
     * @return
     */
    Model.Service[] value();
}
