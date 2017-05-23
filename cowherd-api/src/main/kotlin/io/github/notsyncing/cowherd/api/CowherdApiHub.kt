package io.github.notsyncing.cowherd.api

import java.util.concurrent.ConcurrentHashMap

typealias InstanceWrapper = (Class<*>) -> Any

object CowherdApiHub {
    private val hub = ConcurrentHashMap<String, Any>()
    private val instanceWrappers = ConcurrentHashMap<String, InstanceWrapper>()

    fun getPublishedServices() = hub

    fun publish(serviceClass: Class<*>) {
        hub[serviceClass.name] = serviceClass
    }

    fun publish(serviceClass: Class<*>, instanceWrapper: InstanceWrapper) {
        publish(serviceClass)
        instanceWrappers[serviceClass.name] = instanceWrapper
    }

    fun publish(serviceInstance: Any) {
        hub[serviceInstance.javaClass.name] = serviceInstance
    }

    fun publish(serviceClass: Class<*>, executor: ApiExecutor) {
        hub[serviceClass.name] = executor
    }

    fun publish(serviceName: String, executor: ApiExecutor) {
        hub[serviceName] = executor
    }

    fun revoke(serviceName: String) {
        hub.remove(serviceName)
        instanceWrappers.remove(serviceName)
    }

    fun revoke(serviceClass: Class<*>) {
        revoke(serviceClass.name)
    }

    fun revoke(serviceInstance: Any) {
        revoke(serviceInstance.javaClass.name)
    }

    fun getClass(serviceClassName: String): Class<Any> {
        val s = hub[serviceClassName]

        if (s is Class<*>) {
            return s as Class<Any>
        } else {
            return s!!.javaClass
        }
    }

    fun getInstance(serviceClassName: String): Any? {
        val s = hub[serviceClassName]

        if (s is Class<*>) {
            if (instanceWrappers.containsKey(s.name)) {
                return instanceWrappers[s.name]!!.invoke(s)
            }

            return s.newInstance()
        } else {
            return s
        }
    }

    fun reset() {
        hub.clear()

        CowherdApiGatewayService.reset()
    }
}