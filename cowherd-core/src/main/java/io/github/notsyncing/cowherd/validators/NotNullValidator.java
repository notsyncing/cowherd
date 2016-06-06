package io.github.notsyncing.cowherd.validators;

import io.github.notsyncing.cowherd.validators.annotations.NotNull;

import java.lang.reflect.Parameter;

public class NotNullValidator implements ParameterValidator<NotNull, Object>
{
    @Override
    public boolean validate(Parameter parameter, NotNull validatorAnnotation, Object value)
    {
        return value != null;
    }
}
