package io.github.notsyncing.cowherd.validators.annotations;

import io.github.notsyncing.cowherd.validators.ParameterValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceActionParameterValidator
{
    Class<? extends ParameterValidator> value();
}
