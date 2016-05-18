package io.github.notsyncing.cowherd.authentication;

import io.github.notsyncing.cowherd.models.FilterContext;

import java.lang.annotation.Annotation;
import java.util.concurrent.CompletableFuture;

public interface ActionAuthenticator<A extends Annotation>
{
    CompletableFuture<Boolean> authenticate(A authAnnotation, FilterContext context);
}
