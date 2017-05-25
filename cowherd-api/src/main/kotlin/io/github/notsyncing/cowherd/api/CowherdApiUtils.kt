package io.github.notsyncing.cowherd.api

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.jvmErasure

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
            return JSON.parseObject(str, type.java)
        }
    }

    private fun String.toType(type: KClass<*>) = stringToType(this, type)

    fun expandJsonToMethodParameters(method: KCallable<*>, o: JSONObject?, self: Any?,
                                     specialTypeParameterHandler: ((KParameter) -> Any?)? = null): MutableMap<KParameter, Any?> {
        val targetParams = mutableMapOf<KParameter, Any?>()
        val params = method.parameters

        if (!params.isEmpty()) {
            for (p in params) {
                if (p.kind == KParameter.Kind.INSTANCE) {
                    targetParams[p] = self
                    continue
                } else if (p.kind != KParameter.Kind.VALUE) {
                    continue
                }

                var v = specialTypeParameterHandler?.invoke(p)

                if (v != null) {
                    targetParams[p] = v
                    continue
                }

                if (o?.containsKey(p.name) == true) {
                    val sv = o[p.name].toString()
                    v = sv.toType(p.type.jvmErasure)
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
}