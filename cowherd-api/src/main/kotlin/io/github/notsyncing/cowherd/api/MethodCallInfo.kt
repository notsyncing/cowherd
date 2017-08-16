package io.github.notsyncing.cowherd.api

import kotlin.reflect.KCallable

class MethodCallInfo(val method: KCallable<*>,
                     val serviceClassName: String) {
    val methodParameters = method.parameters.map { MethodParameterInfo(it) }
}