package io.github.notsyncing.cowherd.cluster

import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import java.io.OutputStream
import java.net.URLClassLoader
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger
import kotlin.streams.toList

object Utils {
    private val log = Logger.getLogger("CommandCenterUtils")

    fun readBytes(socket: NetSocket, length: Long, currentBuffer: Buffer, bufferStartPos: Int, callback: (ByteArray) -> Unit): CompletableFuture<Unit> {
        val f = CompletableFuture<Unit>()

        val buf = ByteArray(length.toInt())
        var currentBufIndex = 0

        if (bufferStartPos >= 0) {
            currentBuffer.getBytes(bufferStartPos, currentBuffer.length(), buf)
            currentBufIndex += currentBuffer.length() - bufferStartPos
        }

        if (currentBufIndex >= length) {
            try {
                callback(buf)
                f.complete(Unit)
            } catch (e: Throwable) {
                f.completeExceptionally(e)
            }

            return f
        }

        socket.handler {
            val copyLength = minOf(it.length(), length.toInt() - currentBufIndex)

            if (copyLength <= 0) {
                return@handler
            }

            it.getBytes(0, copyLength, buf, currentBufIndex)
            currentBufIndex += copyLength

            if (currentBufIndex >= length) {
                try {
                    callback(buf)
                    f.complete(Unit)
                } catch (e: Throwable) {
                    f.completeExceptionally(e)
                }
            }
        }

        return f
    }

    fun pipeBytes(socket: NetSocket, outputStream: OutputStream, length: Long, currentBuffer: Buffer,
                  bufferStartPos: Int, callback: (Throwable?) -> Unit): CompletableFuture<Unit> {
        val f = CompletableFuture<Unit>()

        var copiedLength = 0

        if (bufferStartPos >= 0) {
            val buf = ByteArray(currentBuffer.length() - bufferStartPos)
            currentBuffer.getBytes(bufferStartPos, currentBuffer.length(), buf)

            try {
                outputStream.write(buf)
            } catch (e: Exception) {
                callback(e)
                f.completeExceptionally(e)
                return f
            }

            copiedLength += currentBuffer.length() - bufferStartPos
        }

        if (copiedLength >= length) {
            callback(null)
            f.complete(Unit)
            return f
        }

        socket.handler {
            try {
                outputStream.write(it.bytes, 0, it.length())
            } catch (e: Exception) {
                callback(e)
                f.completeExceptionally(e)
                return@handler
            }

            copiedLength += it.length()

            if (copiedLength >= length) {
                callback(null)
                f.complete(Unit)
            }
        }

        socket.exceptionHandler { callback(it) }

        return f
    }

    fun drainBytes(socket: NetSocket) {
        socket.handler {
            val buf = ByteArray(it.length())
            it.getBytes(buf)
        }
    }

    fun getClasspathList(): List<Path> {
        val cl = javaClass.classLoader

        if (cl is URLClassLoader) {
            return cl.getURLs()
                    .filter {
                        if (it.protocol != "file") {
                            log.warning("Unsupported classpath element URL $it, only file protocol is supported")
                        }

                        it.protocol == "file"
                    }
                    .map { Paths.get(it.toURI()) }
        } else {
            throw UnsupportedOperationException("Unsupported classloader type ${javaClass.classLoader.javaClass}")
        }
    }

    fun getFileList(path: Path): List<Path> {
        return Files.list(path)
                .toList()
    }

    private fun createChecksum(file: Path): ByteArray {
        Files.newInputStream(file).use {
            val buffer = ByteArray(1024)
            val complete = MessageDigest.getInstance("MD5")
            var numRead: Int

            do {
                numRead = it.read(buffer)
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead)
                }
            } while (numRead != -1)

            return complete.digest()
        }
    }

    fun getMD5Checksum(file: Path): String {
        val b = createChecksum(file)
        var result = ""

        for (i in b.indices) {
            result += Integer.toString((b[i].toInt() and 0xff) + 0x100, 16).substring(1)
        }

        return result
    }

    fun longToBytes(x: Long): ByteArray {
        val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        buffer.putLong(0, x)
        return buffer.array()
    }

    fun bytesToLong(bytes: ByteArray): Long {
        val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        buffer.put(bytes, 0, bytes.size)
        buffer.flip()
        return buffer.long
    }

    private fun combineByteArrays(one: ByteArray, two: ByteArray): ByteArray {
        val combined = ByteArray(one.size + two.size)

        System.arraycopy(one, 0, combined, 0, one.size)
        System.arraycopy(two, 0, combined, one.size, two.size)

        return combined
    }

    fun writeMessageHeader(socket: NetSocket, header: String, length: Long): NetSocket {
        return socket.write(Buffer.buffer(combineByteArrays(header.toByteArray(), longToBytes(length))))
    }

    fun writeMessage(socket: NetSocket, header: String, data: String): NetSocket {
        val d = data.toByteArray()

        return writeMessageHeader(socket, header, d.size.toLong())
                .write(Buffer.buffer(d))
    }
}