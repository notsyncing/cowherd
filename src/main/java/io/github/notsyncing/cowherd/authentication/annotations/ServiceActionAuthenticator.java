package io.github.notsyncing.cowherd.authentication.annotations;

import io.github.notsyncing.cowherd.authentication.ActionAuthenticator;

import java.lang.annotation.*;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceActionAuthenticator
{
    Class<? extends ActionAuthenticator> value();
}
