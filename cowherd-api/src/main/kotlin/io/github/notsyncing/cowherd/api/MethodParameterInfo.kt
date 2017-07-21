package io.github.notsyncing.cowherd.api

import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType

class MethodParameterInfo(val parameter: KParameter) {
    val javaType = parameter.type.javaType
    val optional = parameter.isOptional
}