package io.github.notsyncing.cowherd.api

import java.lang.invoke.MethodHandle
import java.lang.reflect.Parameter

class MethodCallInfo(val methodHandle: MethodHandle, val parameters: Array<Parameter>) {
}