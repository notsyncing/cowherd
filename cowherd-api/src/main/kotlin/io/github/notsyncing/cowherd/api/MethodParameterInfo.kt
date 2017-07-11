package io.github.notsyncing.cowherd.api

import kotlin.reflect.KParameter
import kotlin.reflect.jvm.jvmErasure

class MethodParameterInfo(val parameter: KParameter) {
    val jvmErasure = parameter.type.jvmErasure
    val optional = parameter.isOptional
}