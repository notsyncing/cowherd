package io.github.notsyncing.cowherd.api

import com.alibaba.fastjson.JSON
import io.github.notsyncing.cowherd.annotations.Exported
import io.github.notsyncing.cowherd.annotations.Parameter
import io.github.notsyncing.cowherd.annotations.Route
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpAnyMethod
import io.github.notsyncing.cowherd.models.ActionContext
import io.github.notsyncing.cowherd.models.Pair
import io.github.notsyncing.cowherd.models.UploadFileInfo
import io.github.notsyncing.cowherd.service.CowherdService
import io.github.notsyncing.cowherd.utils.FutureUtils
import io.vertx.core.http.HttpServerRequest
import java.net.HttpCookie
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class CowherdApiGatewayService : CowherdService() {
    companion object {
        private const val DEFAULT_SERVICE_METHOD = "__default_service_method__"
        private const val ACCESS_TOKEN_NAME = "access_token"
        private const val ACCESS_TOKEN_NAME_2 = "token"

        private val methodCache = ConcurrentHashMap<String, MethodCallInfo>()

        fun reset() {
            methodCache.clear()
        }
    }

    @HttpAnyMethod
    @Exported
    @Route("", subRoute = true)
    fun gateway(@Parameter("path") path: String,
                @Parameter("request") request: HttpServerRequest?,
                @Parameter("context") context: ActionContext,
                @Parameter("__parameters__") __parameters__: List<Pair<String, String>>,
                @Parameter("__cookies__") __cookies__: List<HttpCookie>?,
                @Parameter("__uploads__") __uploads__: List<UploadFileInfo>?): CompletableFuture<Any?> {
        val actionPath = stripParameters(path)
        var serviceClassNamePartEnd = actionPath.indexOf("/")

        if (serviceClassNamePartEnd < 0) {
            serviceClassNamePartEnd = actionPath.length
        }

        val serviceClassNamePart = actionPath.substring(0, serviceClassNamePartEnd)

        if (serviceClassNamePart.isNullOrEmpty()) {
            return FutureUtils.failed(IllegalArgumentException("This request to API gateway has invalid path: $actionPath"))
        }

        val (pt, paramStr) = getEncodedParameters(__parameters__)

        val serviceClassName = serviceClassNamePart

        if (serviceClassName.compareTo("new_session", true) == 0) {
            val session = newSession()

            if (__parameters__.any { (it.key == "cookies") && (it.value == "true") }) {
                val cookie = HttpCookie(ACCESS_TOKEN_NAME, session)

                if (__parameters__.any { (it.key == "remember") && (it.value == "true") }) {
                    cookie.maxAge = Long.MAX_VALUE
                }

                putCookie(request, cookie)
            }

            return CompletableFuture.completedFuture(session)
        }

        val serviceMethodNamePartStart = serviceClassNamePartEnd + 1
        var serviceMethodNamePartEnd = actionPath.indexOf("/", serviceMethodNamePartStart)

        if (serviceMethodNamePartEnd < 0) {
            serviceMethodNamePartEnd = actionPath.length
        }

        val serviceMethodName = if ((serviceMethodNamePartEnd > 0) && (serviceMethodNamePartStart < serviceMethodNamePartEnd))
            path.substring(serviceMethodNamePartStart, serviceMethodNamePartEnd)
        else
            DEFAULT_SERVICE_METHOD

        val service = CowherdApiHub.getInstance(serviceClassName)

        if (service == null) {
            val decodedServiceClassName = URLDecoder.decode(serviceClassName, "utf-8")
            return FutureUtils.failed(IllegalArgumentException("Service class $decodedServiceClassName not found, maybe not published or revoked?"))
        }

        if (service is ApiExecutor) {
            val preferredHttpMethod = service.preferredHttpMethod()

            if (preferredHttpMethod != null) {
                if (request?.method() != preferredHttpMethod) {
                    return FutureUtils.failed(IllegalAccessException("Method $service does not like to be called with ${request?.method()}"))
                }
            }
        }

        val actionId = "$serviceClassName.$serviceMethodName"
        var serviceMethodInfo = methodCache[actionId]

        if (serviceMethodInfo == null) {
            serviceMethodInfo = makeMethodCallInfo(actionId, serviceMethodName, service)

            if (serviceMethodInfo == null) {
                val decodedServiceClassName = URLDecoder.decode(serviceClassName, "utf-8")
                throw IllegalArgumentException("Method $serviceMethodName of service class $decodedServiceClassName not found!")
            }

            methodCache[actionId] = serviceMethodInfo
        }

        val jsonObject = JSON.parseObject(paramStr)

        val targetParams = CowherdApiUtils.expandJsonToMethodParameters(serviceMethodInfo, jsonObject, service) { p ->
            if (p.javaType == UploadFileInfo::class.java) {
                __uploads__?.firstOrNull { it.parameterName == p.parameter.name }
            } else if (p.javaType == ActionContext::class.java) {
                context
            } else {
                null
            }
        }

        var sessionIdentifier: String? = null

        if (request?.headers()?.contains("Authorization") == true) {
            val authHeader = request.getHeader("Authorization")
            sessionIdentifier = authHeader.replace("Bearer ", "")
        } else if (__parameters__.any { (it.key == ACCESS_TOKEN_NAME) || (it.key == ACCESS_TOKEN_NAME_2) }) {
            sessionIdentifier = __parameters__.first { (it.key == ACCESS_TOKEN_NAME) || (it.key == ACCESS_TOKEN_NAME_2) }.value
        } else if (__cookies__?.any { (it.name == ACCESS_TOKEN_NAME) || (it.name == ACCESS_TOKEN_NAME_2) } == true) {
            sessionIdentifier = __cookies__.first { (it.name == ACCESS_TOKEN_NAME) || (it.name == ACCESS_TOKEN_NAME_2) }.value
        }

        val o: Any?

        if (service is ApiExecutor) {
            o = service.execute(serviceMethodInfo.method, targetParams, sessionIdentifier, context, __uploads__)
        } else {
            o = serviceMethodInfo.method.callBy(targetParams)
        }

        if (o is CompletableFuture<*>) {
            return o as CompletableFuture<Any?>
        } else {
            return CompletableFuture.completedFuture(o)
        }
    }

    private fun makeMethodCallInfo(actionId: String, serviceMethodName: String, service: Any): MethodCallInfo? {
        val m = if (serviceMethodName == DEFAULT_SERVICE_METHOD)
            if (service is ApiExecutor)
                service.getDefaultMethod()
            else
                service.javaClass.kotlin.members.firstOrNull { it.annotations.any { it.javaClass == DefaultApiMethod::class.java } }
        else
            service.javaClass.kotlin.members.firstOrNull { it.name == serviceMethodName }

        if (m == null) {
            return null
        }

        return MethodCallInfo(m)
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

    private fun newSession(): String {
        return UUID.randomUUID().toString()
    }
}