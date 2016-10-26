package io.github.notsyncing.cowherd.api

import java.util.concurrent.CompletableFuture
import kotlin.reflect.KCallable

abstract class ApiExecutor {
    abstract fun getDefaultMethod(): KCallable<*>

    abstract fun execute(method: KCallable<*>, args: MutableList<Any?>): CompletableFuture<Any?>
}