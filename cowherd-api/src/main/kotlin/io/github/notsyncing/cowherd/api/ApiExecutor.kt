package io.github.notsyncing.cowherd.api

import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter

abstract class ApiExecutor {
    abstract fun getDefaultMethod(): KCallable<*>

    open fun preferredHttpMethod(): HttpMethod? = null

    abstract fun execute(method: KCallable<*>, args: MutableMap<KParameter, Any?>, sessionIdentifier: String?,
                         request: HttpServerRequest?): CompletableFuture<Any?>
}