package io.github.notsyncing.cowherd.server;

import io.github.notsyncing.cowherd.models.ActionResult;
import io.github.notsyncing.cowherd.models.FilterContext;
import io.github.notsyncing.cowherd.models.FilterExecutionInfo;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 过滤器接口
 * 所有过滤器均应实现此接口
 */
public interface ServiceActionFilter
{
    /**
     * 异步过滤方法
     * 该方法在接收到请求，并处理请求参数之前被调用
     * @param context 过滤器上下文对象
     * @return 包含指示请求是否应当继续被处理的 CompletableFuture 对象
     */
    default CompletableFuture<Boolean> early(FilterContext context)
    {
        return CompletableFuture.completedFuture(true);
    }

    /**
     * 异步过滤方法
     * 该方法在接收到请求并处理完请求参数后，执行请求所对应的服务方法之前被调用
     * @param context 过滤器上下文对象
     * @return 包含指示请求是否应当继续被处理的 CompletableFuture 对象
     */
    default CompletableFuture<Boolean> before(FilterContext context)
    {
        return CompletableFuture.completedFuture(true);
    }

    /**
     * 异步过滤方法
     * 该方法在执行完请求所对应的服务方法之后，将数据写回客户端之前被调用
     * @param context 过滤器上下文对象
     * @return 包含指示请求是否应当继续被处理的 CompletableFuture 对象
     */
    default CompletableFuture<ActionResult> after(FilterContext context)
    {
        return CompletableFuture.completedFuture(context.getResult());
    }
}
