package io.github.notsyncing.cowherd.server;

import java.util.concurrent.CompletableFuture;

/**
 * 过滤器接口
 * 所有过滤器均应实现此接口
 */
public interface ServiceActionFilter
{
    /**
     * 异步过滤方法
     * @return 包含指示请求是否应当继续被处理的 CompletableFuture 对象
     */
    CompletableFuture<Boolean> filter();
}
