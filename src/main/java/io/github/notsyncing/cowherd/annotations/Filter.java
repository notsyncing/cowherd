package io.github.notsyncing.cowherd.annotations;

import io.github.notsyncing.cowherd.models.FilterInfo;
import io.github.notsyncing.cowherd.server.ServiceActionFilter;

import java.lang.annotation.*;
import java.util.List;

/**
 * 指示当前方法要使用的过滤器
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Filters.class)
public @interface Filter
{
    /**
     * 过滤器类型
     */
    Class<? extends ServiceActionFilter> value();
    FilterParameter[] parameters() default {};
}
