package io.github.notsyncing.cowherd.server_renderer

import com.mashape.unirest.http.Unirest
import io.github.notsyncing.cowherd.server.CowherdLogger
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.CompletableFuture

object Connector {
    var port: Int = 46317

    private val logger = CowherdLogger.getInstance(this)

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
        logger.i("Cowherd server renderer: get $href")

        CompletableFuture.supplyAsync {
            val r = Unirest.get("http://localhost:$port/get?url=${URLEncoder.encode(href, "utf-8")}")
                    .asString()

            if (r.status != 200) {
                throw IOException("Failed to fetch $href: ${r.statusText} {${r.body}}")
            }

            r.body
        }.await()
    }

    fun exit() = future {
        Unirest.post("http://localhost:$port/exit")
                .asString()
    }
}