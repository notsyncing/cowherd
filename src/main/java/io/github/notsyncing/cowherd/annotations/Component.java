package io.github.notsyncing.cowherd.annotations;

import io.github.notsyncing.cowherd.service.ComponentInstantiateType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示该类用于依赖注入
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component
{
    /**
     * 该类在用于依赖注入时的实例化方式
     */
    ComponentInstantiateType value() default ComponentInstantiateType.Singleton;

    /**
     * 是否在依赖注入器扫描 Classpath 完成后立即实例化
     * 仅对实例化方式为 {@link ComponentInstantiateType#Singleton} 的类有效
     */
    boolean createEarly() default false;
}
