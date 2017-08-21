package io.github.notsyncing.cowherd.api

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import java.lang.reflect.Type
import kotlin.reflect.KParameter

object CowherdApiUtils {
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
                    v = o[p.parameter.name].toType(p.javaType)
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