package io.github.notsyncing.cowherd.exceptions;

import io.github.notsyncing.cowherd.validators.ParameterValidator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

public class ValidationFailedException extends Exception
{
    private Parameter parameter;
    private ParameterValidator validator;
    private Annotation validatorAnnotation;
    private Object value;

    public ValidationFailedException(Parameter parameter, ParameterValidator validator, Annotation validatorAnnotation, Object value)
    {
        this.parameter = parameter;
        this.validator = validator;
        this.validatorAnnotation = validatorAnnotation;
        this.value = value;
    }

    public Parameter getParameter()
    {
        return parameter;
    }

    public ParameterValidator getValidator()
    {
        return validator;
    }

    public Annotation getValidatorAnnotation()
    {
        return validatorAnnotation;
    }

    public Object getValue()
    {
        return value;
    }
}
