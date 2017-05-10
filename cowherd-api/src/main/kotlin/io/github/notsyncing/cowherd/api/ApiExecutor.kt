package io.github.notsyncing.cowherd.api

import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KCallable

abstract class ApiExecutor {
    abstract fun getDefaultMethod(): KCallable<*>

    fun preferredHttpMethod(): HttpMethod? = null

    abstract fun execute(method: KCallable<*>, args: MutableList<Any?>, sessionIdentifier: String?,
                         request: HttpServerRequest?): CompletableFuture<Any?>
}