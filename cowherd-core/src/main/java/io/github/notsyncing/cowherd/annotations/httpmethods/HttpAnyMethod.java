package io.github.notsyncing.cowherd.annotations.httpmethods;

import io.vertx.core.http.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示该方法可以通过任意 HTTP 请求方式访问
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpAnyMethod
{
    HttpMethod prefer() default HttpMethod.GET;
}
