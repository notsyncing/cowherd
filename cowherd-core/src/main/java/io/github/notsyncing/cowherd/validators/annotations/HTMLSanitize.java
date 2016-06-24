package io.github.notsyncing.cowherd.validators.annotations;

import io.github.notsyncing.cowherd.validators.HTMLSanitizationValidator;
import io.github.notsyncing.cowherd.validators.LengthValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@ServiceActionParameterValidator(HTMLSanitizationValidator.class)
public @interface HTMLSanitize
{
    boolean textOnly() default false;
}
