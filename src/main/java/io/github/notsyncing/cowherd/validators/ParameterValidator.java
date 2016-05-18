package io.github.notsyncing.cowherd.validators;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

public interface ParameterValidator<A extends Annotation, T>
{
    boolean validate(Parameter parameter, A validatorAnnotation, T value);

    default boolean validate(A validatorAnnotation, T value)
    {
        return validate(null, validatorAnnotation, value);
    }

    default boolean validate(T value)
    {
        return validate(null, null, value);
    }
}
