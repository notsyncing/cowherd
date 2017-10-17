package io.github.notsyncing.cowherd.cluster

import io.github.notsyncing.cowherd.Cowherd
import io.github.notsyncing.cowherd.cluster.enums.FileItemType
import io.github.notsyncing.cowherd.cluster.models.ClasspathList
import io.github.notsyncing.cowherd.cluster.models.FileItem
import io.github.notsyncing.cowherd.cluster.models.NodeInfo
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.logging.Level
import java.util.logging.Logger

abstract class CowherdClusterNode {
    protected lateinit var vertx: Vertx

    private var selfVertx = false

    lateinit var selfNode: NodeInfo

    protected var handlingMessage = false

    val localClasspathList = ClasspathList()

    protected val log: Logger = Logger.getLogger(this.javaClass.simpleName)

    open fun init() {
        loadClasspathList()

        val hostName = InetAddress.getLocalHost().hostName ?: "<UNNAMED>"

        val netInterfaces = NetworkInterface.getNetworkInterfaces()
        var macAddr = "<NO NIC>"

        while (netInterfaces.hasMoreElements()) {
            val nif = netInterfaces.nextElement()

            if ((nif.isLoopback) || (!nif.isUp)) {
                continue
            }

            val mac = nif.hardwareAddress
            val sb = StringBuilder()

            for (i in 0 until mac.size) {
                sb.append(String.format("%02X%s", mac[i], if (i < mac.size - 1) "-" else ""))
            }

            macAddr = sb.toString()

            break
        }

        selfNode = NodeInfo(hostName, macAddr, 0, "", "", 0, 0)

        if (Cowherd.dependencyInjector != null) {
            vertx = Cowherd.dependencyInjector.getComponent(Vertx::class.java)
            selfVertx = false
        } else {
            vertx = Vertx.vertx()
            selfVertx = true
        }
    }

    protected fun loadClasspathList() {
        localClasspathList.clear()

        val list = Utils.getClasspathList()

        list.filter {
            !ClusterConfigs.isJarBlacklisted(it.fileName.toString())
        }.filter {
            (!ClusterConfigs.ignoreDirectoryInClasspath) || (!Files.isDirectory(it))
        }.map {
            val type = if (Files.isDirectory(it)) {
                FileItemType.Directory
            } else {
                FileItemType.File
            }

            val checksum = if (type == FileItemType.File) {
                Utils.getMD5Checksum(it)
            } else {
                it.toString()
            }

            FileItem(it.toAbsolutePath().toString(), type, checksum)
        }.forEach { localClasspathList.add(it) }
    }

    protected fun handleMessageHeader(socket: NetSocket): (Buffer) -> Unit {
        val headBuf = ByteArray(12)
        var currentBufIndex = 0
        var currentType: String? = null
        var payloadLength: Long = 0

        return h@ { it: Buffer ->
            //log.info("Data: ${it.toString(Charsets.UTF_8)}")

            var bufStartPos = 0

            if (currentType == null) {
                if (it.length() < 12 - currentBufIndex) {
                    val end = minOf(it.length(), 12 - currentBufIndex)
                    it.getBytes(0, end, headBuf, currentBufIndex)
                    currentBufIndex += end

                    if (currentBufIndex < 12) {
                        return@h
                    }

                    bufStartPos = -1
                } else {
                    it.getBytes(0, 12 - currentBufIndex, headBuf, currentBufIndex)
                    bufStartPos += 12 - currentBufIndex
                }

                payloadLength = ByteBuffer.wrap(headBuf.copyOfRange(4, 12)).long
                currentType = String(headBuf, 0, 4)
            }

            if (currentType != null) {
                handlingMessage = true

                val type = currentType!!
                val len = payloadLength

                currentType = null
                currentBufIndex = 0
                payloadLength = 0

                try {
                    handleMessage(socket, type, len, it, bufStartPos)
                } catch (e: Throwable) {
                    log.log(Level.WARNING, "An exception occured when handling message", e)
                }

                handlingMessage = false
            } else {
                log.warning("Received cluster message with no type")
                socket.close()
            }
        }
    }

    protected abstract fun handleMessage(socket: NetSocket, type: String, payloadLength: Long, currentBuffer: Buffer,
                                         bufferStartPos: Int)

    protected open fun handleMessageConnectionException(socket: NetSocket) = { it: Throwable ->
        log.log(Level.WARNING, "An exception occured in data connection to ${socket.remoteAddress()}", it)
    }

    protected fun readAllAndExecuteAsync(socket: NetSocket, payloadLength: Long, currentBuffer: Buffer,
                                         bufferStartPos: Int, block: (ByteArray) -> Unit) {
        val done = {
            socket.handler(handleMessageHeader(socket))
            socket.exceptionHandler(handleMessageConnectionException(socket))
        }

        Utils.readBytes(socket, payloadLength, currentBuffer, bufferStartPos) {
            done()

            CompletableFuture.runAsync {
                block(it)
            }.exceptionally {
                log.log(Level.WARNING, "An exception occured when receiving cmd", it)
                null
            }
        }.exceptionally {
            log.log(Level.WARNING, "An exception occured when receiving cmd", it)
        }
    }

    open fun destroy() {
        if (selfVertx) {
            vertx.close()
        }
    }
}