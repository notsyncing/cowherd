package io.github.notsyncing.cowherd.flex

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.cowherd.annotations.Exported
import io.github.notsyncing.cowherd.annotations.Parameter
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpAnyMethod
import io.github.notsyncing.cowherd.models.ActionContext
import io.github.notsyncing.cowherd.models.Pair
import io.github.notsyncing.cowherd.models.UploadFileInfo
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.net.HttpCookie
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.jvmErasure

object ScriptActionInvoker {
    fun anyToType(any: Any?, type: Type): Any? {
        if (type == Int::class.java) {
            if (any is Int) {
                return any
            } else {
                return any.toString().toInt()
            }
        } else if (type == Long::class.java) {
            if (any is Long) {
                return any
            } else {
                return any.toString().toLong()
            }
        } else if (type == String::class.java) {
            if (any is String) {
                return any
            } else {
                return any.toString()
            }
        } else if (type == Boolean::class.java) {
            if (any is Boolean) {
                return any
            } else {
                return any.toString().toBoolean()
            }
        } else if (type == Float::class.java) {
            if (any is Float) {
                return any
            } else {
                return any.toString().toFloat()
            }
        } else if (type == Double::class.java) {
            if (any is Double) {
                return any
            } else {
                return any.toString().toDouble()
            }
        } else if (type == Byte::class.java) {
            if (any is Byte) {
                return any
            } else {
                return any.toString().toByte()
            }
        } else if (type == Char::class.java) {
            if (any is Char) {
                return any
            } else {
                return any.toString().toInt()
            }
        } else if (type == Short::class.java) {
            if (any is Short) {
                return any
            } else {
                return any.toString().toShort()
            }
        } else if (type == JSONObject::class.java) {
            if (any is JSONObject) {
                return any
            } else {
                return JSON.parse(any.toString())
            }
        } else if (type == JSONArray::class.java) {
            if (any is JSONArray) {
                return any
            } else {
                return JSON.parseArray(any.toString())
            }
        } else {
            val s: String
            val from = any.toString()

            if ((from.startsWith("[")) && (from.endsWith("]"))) {
                s = from
            } else if ((from.startsWith("{")) && (from.endsWith("}"))) {
                s = from
            } else {
                s = "\"$from\""
            }

            return JSON.parseObject(s, type)
        }
    }

    private fun Any?.toType(type: Type) = anyToType(this, type)

    private fun processParameters(function: KFunction<*>, request: HttpServerRequest, response: HttpServerResponse,
                                  parameters: List<Pair<String, String>>, cookies: List<HttpCookie>,
                                  uploads: List<UploadFileInfo>): Map<KParameter, Any?> {
        val targetParams = mutableMapOf<KParameter, Any?>()
        val params = function.parameters
        val o = mutableMapOf<String, String>()
        val uploadMap = uploads.groupBy { it.parameterName }

        parameters.forEach { (k, v) -> o[k] = v }
        cookies.forEach { o[it.name] = it.value }

        if (!params.isEmpty()) {
            for (p in params) {
                if (p.kind == KParameter.Kind.INSTANCE) {
                    continue
                } else if (p.kind != KParameter.Kind.VALUE) {
                    continue
                }

                val v: Any?
                val type = p.type.jvmErasure.java
                val typeName = type.name

                if (typeName == request.javaClass.typeName) {
                    v = request
                } else if (typeName == response.javaClass.typeName) {
                    v = response
                } else if (typeName == uploadMap::class.qualifiedName) {
                    v = uploadMap
                } else if (typeName == HttpCookie::class.qualifiedName) {
                    v = cookies.firstOrNull { it.name == p.name }
                } else if (o.containsKey(p.name)) {
                    v = o[p.name].toType(type)
                } else {
                    if (p.isOptional) {
                        continue
                    }

                    v = null
                }

                targetParams[p] = v
            }
        }

        return targetParams
    }

    @Exported
    @HttpAnyMethod
    @JvmStatic
    fun invokeAction(context: ActionContext,
                     request: HttpServerRequest,
                     response: HttpServerResponse,
                     @Parameter("__parameters__") __parameters__: List<Pair<String, String>>,
                     @Parameter("__cookies__") __cookies__: List<HttpCookie>?,
                     @Parameter("__uploads__") __uploads__: List<UploadFileInfo>?): Any? {
        val function = context.route.getTag(CowherdScriptManager.TAG_FUNCTION) as KFunction<*>?
        val fc = context.route.getTag(CowherdScriptManager.TAG_FUNCTION_CLASS)
        val realFunction = context.route.getTag(CowherdScriptManager.TAG_REAL_FUNCTION) as Method
        val httpMethod = context.route.getTag(CowherdScriptManager.TAG_HTTP_METHOD) as HttpMethod?

        if (function == null) {
            throw ClassNotFoundException("No function associated with route ${context.route.path}")
        }

        if ((httpMethod != null) && (httpMethod != request.method())) {
            response.statusCode = 403
            response.write("Route ${context.route.path} is not allowed to be accessed with method ${request.rawMethod()}")
            response.end()
            return null
        }

        val params = processParameters(function, request, response, __parameters__, __cookies__ ?: emptyList(),
                __uploads__ ?: emptyList())

        val actualParams = Array<Any?>(params.size) { null }

        for (p in params) {
            actualParams[p.key.index] = p.value
        }

        return realFunction.invoke(fc, *actualParams)
    }
}