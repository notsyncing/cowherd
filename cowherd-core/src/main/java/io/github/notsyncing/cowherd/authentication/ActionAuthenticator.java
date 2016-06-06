package io.github.notsyncing.cowherd.authentication;

import io.github.notsyncing.cowherd.models.FilterContext;

import java.lang.annotation.Annotation;
import java.util.concurrent.CompletableFuture;

/**
 * 服务请求异步验证器接口
 * @param <A> 该验证器所对应的注解类
 */
public interface ActionAuthenticator<A extends Annotation>
{
    /**
     * 异步验证方法
     * @param authAnnotation 该验证器所对应的注解类实例
     * @param context 过滤器上下文对象
     * @return 指示请求是否验证通过的 CompletableFuture 对象
     */
    CompletableFuture<Boolean> authenticate(A authAnnotation, FilterContext context);
}
