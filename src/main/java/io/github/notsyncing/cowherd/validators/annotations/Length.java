package io.github.notsyncing.cowherd.validators.annotations;

import io.github.notsyncing.cowherd.validators.LengthValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@ServiceActionParameterValidator(LengthValidator.class)
public @interface Length
{
    int value() default -1;
    int max() default -1;
    int min() default -1;
}
