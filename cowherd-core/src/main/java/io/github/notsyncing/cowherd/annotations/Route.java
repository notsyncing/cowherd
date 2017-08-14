package io.github.notsyncing.cowherd.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示当前服务或方法的路由信息
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Route
{
    /**
     * 用于路径匹配的正则表达式
     */
    String value();

    /**
     * 用于域名匹配的正则表达式
     */
    String domain() default "";

    /**
     * 是否为默认路由，即访问 / 时的路由
     */
    boolean entry() default false;

    /**
     * 指向一个视图时，指定视图的路径
     */
    String viewPath() default "";

    /**
     * 是否子路径，即在上一级路径之后附加的路径
     */
    boolean subRoute() default false;

    /**
     * 是否使用简单路由匹配
     */
    boolean fastRoute() default false;
}
