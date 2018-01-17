package io.github.notsyncing.cowherd.micron.config

import com.alibaba.fastjson.JSONObject

interface ConfigProvider {
    fun load(preferredName: String, env: String?): Boolean

    fun load(preferredName: String): Boolean {
        return load(preferredName, null)
    }

    fun save(preferredName: String, env: String?): Boolean

    fun save(preferredName: String): Boolean {
        return save(preferredName, null)
    }

    fun get(): JSONObject?

    fun <T> get(clazz: Class<T>): T? {
        return get()?.toJavaObject(clazz)
    }

    fun <T> get(key: String, clazz: Class<T>): T? {
        return get()?.getJSONObject(key)?.toJavaObject(clazz)
    }
}