package io.github.notsyncing.cowherd.api

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.jvmErasure

object CowherdApiUtils {
    private fun String.toType(type: KClass<*>): Any? {
        if (type == Int::class) {
            return this.toInt()
        } else if (type == Long::class) {
            return this.toLong()
        } else if (type == String::class) {
            return this
        } else if (type == Boolean::class) {
            return this.toBoolean()
        } else if (type == Float::class) {
            return this.toFloat()
        } else if (type == Double::class) {
            return this.toDouble()
        } else if (type == Byte::class) {
            return this.toByte()
        } else if (type == Char::class) {
            return this.toInt()
        } else if (type == Short::class) {
            return this.toShort()
        } else {
            return JSON.parseObject(this, type.java)
        }
    }

    fun expandJsonToMethodParameters(method: KCallable<*>, o: JSONObject?): MutableList<Any?> {
        if (o == null) {
            return mutableListOf()
        }

        val targetParams = ArrayList<Any?>()
        val params = method.parameters

        if (!params.isEmpty()) {
            for (p in params) {
                if (p.kind != KParameter.Kind.VALUE) {
                    continue
                }

                val sv = o[p.name].toString()
                val v = sv.toType(p.type.jvmErasure)

                targetParams.add(v)
            }
        }

        return targetParams
    }
}