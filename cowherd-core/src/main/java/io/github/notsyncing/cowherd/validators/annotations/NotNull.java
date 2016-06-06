package io.github.notsyncing.cowherd.validators.annotations;

import io.github.notsyncing.cowherd.validators.NotNullValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@ServiceActionParameterValidator(NotNullValidator.class)
public @interface NotNull
{
}
