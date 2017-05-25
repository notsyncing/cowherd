package io.github.notsyncing.cowherd.api

import io.github.notsyncing.cowherd.models.ActionContext
import io.github.notsyncing.cowherd.models.UploadFileInfo
import io.vertx.core.http.HttpMethod
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter

abstract class ApiExecutor {
    abstract fun getDefaultMethod(): KCallable<*>

    open fun preferredHttpMethod(): HttpMethod? = null

    abstract fun execute(method: KCallable<*>, args: MutableMap<KParameter, Any?>, sessionIdentifier: String?,
                         context: ActionContext, uploads: List<UploadFileInfo>?): CompletableFuture<Any?>
}