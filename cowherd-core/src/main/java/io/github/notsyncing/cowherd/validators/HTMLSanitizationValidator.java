package io.github.notsyncing.cowherd.validators;

import io.github.notsyncing.cowherd.validators.annotations.HTMLSanitize;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.lang.reflect.Parameter;

public class HTMLSanitizationValidator implements ParameterValidator<HTMLSanitize, String>
{
    @Override
    public String filter(Parameter parameter, HTMLSanitize validationAnnotation, String value)
    {
        if (value == null) {
            return null;
        }

        if (validationAnnotation.textOnly()) {
            return Jsoup.clean(value, Whitelist.none());
        } else {
            return Jsoup.clean(value, Whitelist.relaxed());
        }
    }
}
