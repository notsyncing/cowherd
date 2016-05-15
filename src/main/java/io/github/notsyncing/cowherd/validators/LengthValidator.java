package io.github.notsyncing.cowherd.validators;

import io.github.notsyncing.cowherd.validators.annotations.Length;

import java.lang.reflect.Parameter;

public class LengthValidator implements ParameterValidator<Length, String>
{
    @Override
    public boolean validate(Parameter parameter, Length validatorAnnotation, String value)
    {
        if (validatorAnnotation.value() > 0) {
            return ((value != null) && (value.length() == validatorAnnotation.value()));
        }

        if ((validatorAnnotation.max() > 0) && ((value != null) && (value.length() > validatorAnnotation.max()))) {
            return false;
        }

        if ((validatorAnnotation.min() > 0) && ((value == null) || (value.length() < validatorAnnotation.min()))) {
            return false;
        }

        return true;
    }
}
