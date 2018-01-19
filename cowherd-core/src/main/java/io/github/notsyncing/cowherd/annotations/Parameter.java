package io.github.notsyncing.cowherd.annotations;

import io.github.notsyncing.cowherd.commons.ParameterParseType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
    String value() default "";
    ParameterParseType parseType() default ParameterParseType.Normal;
}
