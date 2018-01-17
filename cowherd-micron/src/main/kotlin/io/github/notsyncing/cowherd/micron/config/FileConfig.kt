package io.github.notsyncing.cowherd.micron.config

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import java.nio.file.Files
import java.nio.file.Paths

class FileConfig : ConfigProvider {
    private var config: JSONObject? = null
    private var loadedFromFile = false

    private fun makeFileName(preferredName: String, env: String?): String {
        return preferredName + (if (env != null) ".$env" else "") + ".json"
    }

    private fun loadFromClasspath(preferredName: String, env: String?): Boolean {
        val url = javaClass.getResource("/${makeFileName(preferredName, env)}")

        if (url == null) {
            return false
        }

        url.openStream().use {
            config = JSON.parseObject(it.bufferedReader(Charsets.UTF_8).readText())
        }

        loadedFromFile = false
        return true
    }

    private fun loadFromFile(preferredName: String, env: String?): Boolean {
        val p = Paths.get("./${makeFileName(preferredName, env)}")

        if (!Files.exists(p)) {
            return false
        }

        val bytes = Files.readAllBytes(p)
        config = JSON.parseObject(String(bytes, Charsets.UTF_8))

        loadedFromFile = true
        return true
    }

    override fun load(preferredName: String, env: String?): Boolean {
        if (!loadFromFile(preferredName, env)) {
            return loadFromClasspath(preferredName, env)
        }

        return true
    }

    override fun save(preferredName: String, env: String?): Boolean {
        if (config == null) {
            return false
        }

        var p = Paths.get("./${makeFileName(preferredName, env)}")

        if (!Files.exists(p)) {
            p = Files.createFile(p)
        }

        Files.newBufferedWriter(p).use {
            it.write(JSON.toJSONString(config!!, true))
        }

        return true
    }

    override fun get(): JSONObject? {
        return config
    }
}