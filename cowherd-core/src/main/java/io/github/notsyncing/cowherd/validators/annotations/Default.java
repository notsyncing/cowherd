package io.github.notsyncing.cowherd.validators.annotations;

import io.github.notsyncing.cowherd.validators.DefaultValidator;
import io.github.notsyncing.cowherd.validators.HTMLSanitizationValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@ServiceActionParameterValidator(DefaultValidator.class)
public @interface Default
{
    String value();
}
