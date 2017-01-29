package io.github.notsyncing.cowherd.server_renderer

import com.mashape.unirest.http.Unirest
import kotlinx.coroutines.experimental.future.future
import java.io.IOException

object Connector {
    var port: Int = 46317

    fun ping(): Boolean {
        val ret = Unirest.get("http://localhost:$port/ping")
                .asString()
                .body

        return ret == "Alive"
    }

    fun ping(retryCount: Int, delay: Long = 0): Boolean {
        if (retryCount <= 0) {
            return false
        }

        if (delay > 0) {
            Thread.sleep(delay)
        }

        try {
            val r = ping()

            if (!r) {
                return ping(retryCount - 1)
            }

            return true
        } catch (e: Exception) {
            return ping(retryCount - 1)
        }
    }

    fun get(href: String) = future<String> {
        val r = Unirest.get("http://localhost:$port/get?url=$href")
                .asString()

        if (r.status != 200) {
            throw IOException("Failed to fetch $href: ${r.statusText} {${r.body}}")
        }

        r.body
    }

    fun exit() = future {
        Unirest.post("http://localhost:$port/exit")
                .asString()
    }
}