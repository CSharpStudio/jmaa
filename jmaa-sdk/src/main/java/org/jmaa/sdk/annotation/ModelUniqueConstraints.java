package org.jmaa.sdk.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.jmaa.sdk.Model;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Eric Liang
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelUniqueConstraints {
    /**
     * 值
     *
     * @return
     */
    Model.UniqueConstraint[] value();
}
