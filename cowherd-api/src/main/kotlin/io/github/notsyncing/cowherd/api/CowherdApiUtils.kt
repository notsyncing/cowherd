package io.github.notsyncing.cowherd.api

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

object CowherdApiUtils {
    fun stringToType(str: String, type: KClass<*>): Any? {
        if (type == Int::class) {
            return str.toInt()
        } else if (type == Long::class) {
            return str.toLong()
        } else if (type == String::class) {
            return str
        } else if (type == Boolean::class) {
            return str.toBoolean()
        } else if (type == Float::class) {
            return str.toFloat()
        } else if (type == Double::class) {
            return str.toDouble()
        } else if (type == Byte::class) {
            return str.toByte()
        } else if (type == Char::class) {
            return str.toInt()
        } else if (type == Short::class) {
            return str.toShort()
        } else {
            val s: String

            if ((str.startsWith("[")) && (str.endsWith("]"))) {
                s = str
            } else if ((str.startsWith("{")) && (str.endsWith("}"))) {
                s = str
            } else {
                s = "\"$str\""
            }

            return JSON.parseObject(s, type.java)
        }
    }

    private fun String.toType(type: KClass<*>) = stringToType(this, type)

    fun expandJsonToMethodParameters(info: MethodCallInfo, o: JSONObject?, self: Any?,
                                     specialTypeParameterHandler: ((MethodParameterInfo) -> Any?)? = null): MutableMap<KParameter, Any?> {
        val targetParams = mutableMapOf<KParameter, Any?>()
        val params = info.methodParameters

        if (!params.isEmpty()) {
            for (p in params) {
                if (p.parameter.kind == KParameter.Kind.INSTANCE) {
                    targetParams[p.parameter] = self
                    continue
                } else if (p.parameter.kind != KParameter.Kind.VALUE) {
                    continue
                }

                var v = specialTypeParameterHandler?.invoke(p)

                if (v != null) {
                    targetParams[p.parameter] = v
                    continue
                }

                if (o?.containsKey(p.parameter.name) == true) {
                    val sv = o[p.parameter.name].toString()
                    v = sv.toType(p.jvmErasure)
                } else {
                    if (p.optional) {
                        continue
                    }

                    v = null
                }

                targetParams[p.parameter] = v
            }
        }

        return targetParams
    }
}