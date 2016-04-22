package io.github.notsyncing.cowherd.annotations;

import io.github.notsyncing.cowherd.service.ComponentInstantiateType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component
{
    ComponentInstantiateType value() default ComponentInstantiateType.Singleton;
    boolean createEarly() default false;
}
