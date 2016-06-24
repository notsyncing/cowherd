package io.github.notsyncing.cowherd.validators;

import io.github.notsyncing.cowherd.utils.TypeUtils;
import io.github.notsyncing.cowherd.validators.annotations.Default;

import java.lang.reflect.Parameter;

public class DefaultValidator implements ParameterValidator<Default, Object>
{
    @Override
    public Object filter(Parameter parameter, Default validationAnnotation, Object value)
    {
        if (value != null) {
            return value;
        }

        String defValueStr = validationAnnotation.value();

        return TypeUtils.stringToType(parameter.getType(), defValueStr);
    }
}
