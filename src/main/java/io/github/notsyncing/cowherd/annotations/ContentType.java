package io.github.notsyncing.cowherd.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示当前方法的响应内容类型
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ContentType
{
    /**
     * 当前方法的响应内容类型
     */
    String value();
}
