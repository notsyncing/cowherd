package io.github.notsyncing.cowherd.api

import io.github.notsyncing.cowherd.api.CowherdApiHub.InstanceWrapper
import java.util.concurrent.ConcurrentHashMap

object CowherdApiHub {
    typealias InstanceWrapper = (Class<*>) -> Any

    private val hub = ConcurrentHashMap<String, Any>()
    private val instanceWrappers = ConcurrentHashMap<String, InstanceWrapper>()

    fun publish(serviceClass: Class<*>) {
        hub[serviceClass.canonicalName] = serviceClass
    }

    fun publish(serviceClass: Class<*>, instanceWrapper: InstanceWrapper) {
        publish(serviceClass)
        instanceWrappers[serviceClass.canonicalName] = instanceWrapper
    }

    fun publish(serviceInstance: Any) {
        hub[serviceInstance.javaClass.canonicalName] = serviceInstance
    }

    fun revoke(serviceClassName: String) {
        hub.remove(serviceClassName)
        instanceWrappers.remove(serviceClassName)
    }

    fun revoke(serviceClass: Class<*>) {
        revoke(serviceClass.canonicalName)
    }

    fun revoke(serviceInstance: Any) {
        revoke(serviceInstance.javaClass.canonicalName)
    }

    fun getClass(serviceClassName: String): Class<Any> {
        val s = hub[serviceClassName]

        if (s is Class<*>) {
            return s as Class<Any>
        } else {
            return s!!.javaClass
        }
    }

    fun getInstance(serviceClassName: String): Any {
        val s = hub[serviceClassName]

        if (s is Class<*>) {
            if (instanceWrappers.containsKey(s.canonicalName)) {
                return instanceWrappers[s.canonicalName]!!.invoke(s)
            }

            return s.newInstance()
        } else {
            return s!!
        }
    }

    fun reset() {
        hub.clear()
    }
}