package io.github.notsyncing.cowherd.validators;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

public interface ParameterValidator<A extends Annotation, T>
{
    default boolean validate(Parameter parameter, A validatorAnnotation, T value)
    {
        return true;
    }

    default T filter(Parameter parameter, A validationAnnotation, T value)
    {
        return value;
    }

    default boolean validate(A validatorAnnotation, T value)
    {
        return validate(null, validatorAnnotation, value);
    }

    default boolean validate(T value)
    {
        return validate(null, null, value);
    }

    default T filter(A validatorAnnotation, T value)
    {
        return filter(null, validatorAnnotation, value);
    }

    default T filter(T value)
    {
        return filter(null, null, value);
    }
}
