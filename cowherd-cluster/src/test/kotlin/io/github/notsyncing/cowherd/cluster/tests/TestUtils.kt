package io.github.notsyncing.cowherd.cluster.tests

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import io.vertx.ext.unit.TestContext

object TestUtils {
    fun connectTo(vertx: Vertx, context: TestContext, port: Int, write: (NetSocket) -> Unit,
                  response: (Buffer, NetSocket, (Throwable?) -> Unit) -> Unit) {
        val async = context.async()
        val client = vertx.createNetClient()

        client.connect(port, "localhost") {
            if (it.failed()) {
                context.fail(it.cause())
                async.complete()
                return@connect
            }

            val s = it.result()

            write(s)

            val done = { ex: Throwable? ->
                if (ex != null) {
                    context.fail(ex)
                }

                async.complete()

                s.close()
                client.close()
            }

            s.handler {
                try {
                    response(it, s, done)
                } catch (e: Throwable) {
                    context.fail(e)

                    s.close()
                    client.close()

                    async.complete()
                }
            }
        }
    }
}