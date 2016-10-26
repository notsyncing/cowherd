package io.github.notsyncing.cowherd.api

import com.alibaba.fastjson.JSON
import io.github.notsyncing.cowherd.annotations.Exported
import io.github.notsyncing.cowherd.annotations.Parameter
import io.github.notsyncing.cowherd.annotations.Route
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpAnyMethod
import io.github.notsyncing.cowherd.models.Pair
import io.github.notsyncing.cowherd.models.UploadFileInfo
import io.github.notsyncing.cowherd.service.CowherdService
import io.github.notsyncing.cowherd.utils.FutureUtils
import io.vertx.core.http.HttpServerRequest
import java.lang.invoke.MethodHandles
import java.net.HttpCookie
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class CowherdApiGatewayService : CowherdService() {
    companion object {
        private val methodCache = ConcurrentHashMap<String, MethodCallInfo>()
    }

    @HttpAnyMethod
    @Exported
    @Route("", subRoute = true)
    fun gateway(@Parameter("path") path: String,
                @Parameter("request") request: HttpServerRequest,
                @Parameter("__parameters__") __parameters__: List<Pair<String, String>>,
                @Parameter("__cookies__") __cookies__: List<HttpCookie>?,
                @Parameter("__uploads__") __uploads__: List<UploadFileInfo>?): CompletableFuture<Any?> {
        val actionPath = stripParameters(path)
        val parts = actionPath.split("/")

        if (parts.size != 2) {
            return FutureUtils.failed(IllegalArgumentException("This request to API gateway has invalid path: $actionPath"))
        }

        val (pt, paramStr) = getEncodedParameters(__parameters__)

        val serviceClassName = parts[0]
        val serviceMethodName = parts[1]

        val service = CowherdApiHub.getInstance(serviceClassName)
        val actionId = "$serviceClassName.$serviceMethodName"
        var serviceMethodInfo = methodCache[actionId]

        if (serviceMethodInfo == null) {
            val m = service.javaClass.methods.firstOrNull { it.name == serviceMethodName }

            if (m == null) {
                return FutureUtils.failed(IllegalArgumentException("Method $serviceMethodName of service class $serviceClassName not found!"))
            }

            val mh = MethodHandles.lookup().unreflect(m)
            val info = MethodCallInfo(mh, m.parameters)

            serviceMethodInfo = info
            methodCache[actionId] = info
        }

        val jsonObject = JSON.parseObject(paramStr)
        val targetParams = ArrayList<Any>()

        targetParams.add(service)

        for (p in serviceMethodInfo.parameters) {
            val sv = jsonObject[p.name].toString()
            val v = JSON.parseObject(sv, p.type)

            targetParams.add(v)
        }

        val o = serviceMethodInfo.methodHandle.invokeWithArguments(targetParams)

        if (o is CompletableFuture<*>) {
            return o as CompletableFuture<Any?>
        } else {
            return CompletableFuture.completedFuture(o)
        }
    }

    private fun stripParameters(path: String): String {
        val pathEnd = path.indexOf('?')

        if (pathEnd > 0) {
            return path.substring(0, pathEnd)
        } else {
            return path
        }
    }

    private fun getEncodedParameters(parameters: List<Pair<String, String>>): kotlin.Pair<ParameterEncodeType, String> {
        for (p in parameters) {
            if (p.key == "json") {
                return kotlin.Pair(ParameterEncodeType.Json, p.value)
            }
        }

        return kotlin.Pair(ParameterEncodeType.Unknown, "")
    }
}