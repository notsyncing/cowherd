package io.github.notsyncing.cowherd.annotations;

import io.github.notsyncing.cowherd.service.ComponentInstantiateType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示当前服务的实例化方式
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InstantiateType
{
    /**
     * 当前服务的实例化方式
     */
    ComponentInstantiateType value();
}
