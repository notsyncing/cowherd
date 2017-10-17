package io.github.notsyncing.cowherd.cluster

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.cowherd.Cowherd
import io.github.notsyncing.cowherd.cluster.models.FileItem
import io.github.notsyncing.cowherd.cluster.models.NodeInfo
import io.github.notsyncing.cowherd.commons.CowherdConfiguration
import io.github.notsyncing.cowherd.models.RequestDelegationInfo
import io.github.notsyncing.cowherd.models.RequestDoneInfo
import io.github.notsyncing.cowherd.server.CowherdServer
import io.vertx.core.Context
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpVersion
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Level
import kotlin.streams.toList

class CowherdClusterMaster : CowherdClusterNode() {
    private lateinit var cmdConnection: NetServer
    private lateinit var dataConnection: HttpClient
    private lateinit var dataConnectionContext: Context

    val nodes = ConcurrentHashMap<String, NodeInfo>()
    private val nodeSyncQueue = LinkedBlockingQueue<NodeInfo>()

    override fun init() {
        if (System.getProperty("cowherd.cluster.mode") == "slave") {
            log.info("I'm a slave node.")
            return
        }

        val config = CowherdConfiguration.getRawConfiguration().getJsonObject("cluster")

        super.init()

        initMasterNode(config.getString("listenIp"), config.getInteger("listenPort"))

        val server = Cowherd.dependencyInjector.getComponent(CowherdServer::class.java)
        server.setRequestDelegation(this::redirectRequestToNodes)
        server.setRequestDoneListener(this::masterRequestDone)
    }

    override fun destroy() {
        dataConnection.close()

        cmdConnection.close()

        nodes.clear()
        nodeSyncQueue.clear()

        super.destroy()
    }

    override fun handleMessage(socket: NetSocket, type: String, payloadLength: Long, currentBuffer: Buffer, bufferStartPos: Int) {
        when (type) {
            ClusterConfigs.PKH_PING -> {
                Utils.drainBytes(socket)

                Utils.writeMessageHeader(socket, ClusterConfigs.PKH_PONG, 0L)
            }

            ClusterConfigs.PKH_PONG -> {
                Utils.drainBytes(socket)
            }

            ClusterConfigs.PKH_INFO -> {
                readAllAndExecuteAsync(socket, payloadLength, currentBuffer, bufferStartPos) {
                    val data = String(it)

                    log.info("Received slave node info: $data")

                    val nodes = JSON.parseArray(data)

                    for (i in 0 until nodes.size) {
                        val n = updateNode(socket, nodes.getJSONObject(i))

                        queueReportClasspathListToNode(n)
                    }
                }
            }

            ClusterConfigs.PKH_REQUEST_FILE -> {
                readAllAndExecuteAsync(socket, payloadLength, currentBuffer, bufferStartPos) {
                    val path = String(it)
                    val p = Paths.get(path)

                    log.info("Sending file $path to slave node at ${socket.remoteAddress().host()}")

                    Utils.writeMessageHeader(socket, ClusterConfigs.PKH_FILE, Files.size(p))

                    socket.sendFile(path) {
                        if (it.succeeded()) {
                            log.info("Sent file $path to slave node at ${socket.remoteAddress().host()}")
                        } else {
                            log.log(Level.WARNING, "An exception occured when sending file $path to slave node " +
                                    "at ${socket.remoteAddress().host()}", it.cause())
                        }
                    }
                }
            }

            ClusterConfigs.PKH_REQUEST_FILE_LIST -> {
                readAllAndExecuteAsync(socket, payloadLength, currentBuffer, bufferStartPos) {
                    val path = String(it)
                    val p = Paths.get(path)

                    val fileList = Files.list(p)
                            .map { FileItem(it, "") }
                            .toList()

                    val data = JSON.toJSONString(fileList).toByteArray()

                    Utils.writeMessageHeader(socket, ClusterConfigs.PKH_FILE_LIST, data.size.toLong())
                            .write(Buffer.buffer(data))

                    log.info("Sent file list of path $p to slave node.")
                }
            }

            ClusterConfigs.PKH_SYNCHRONIZE_DONE -> {
                readAllAndExecuteAsync(socket, payloadLength, currentBuffer, bufferStartPos) {
                    val nodeId = String(it)
                    val node = nodes[nodeId]

                    if (node == null) {
                        log.warning("Received synchronize done message, but I don't know a node with id $nodeId")
                        return@readAllAndExecuteAsync
                    }

                    log.info("Received synchronize done message: from node ${node.name} (${node.identifier})")

                    node.ready = true

                    Utils.writeMessageHeader(socket, ClusterConfigs.PKH_PONG, 0L)

                    var nextNode = nodeSyncQueue.poll()

                    if (nextNode == null) {
                        return@readAllAndExecuteAsync
                    }

                    while (nextNode.cmdConnection == socket) {
                        nextNode = nodeSyncQueue.poll()

                        if (nextNode == null) {
                            break
                        }
                    }

                    if (nextNode == null) {
                        return@readAllAndExecuteAsync
                    }

                    reportClasspathListToNode(nextNode)
                }
            }

            ClusterConfigs.PKH_EXIT -> {
                readAllAndExecuteAsync(socket, payloadLength, currentBuffer, bufferStartPos) {
                    val nodeId = String(it)
                    val node = nodes.remove(nodeId)

                    if (node == null) {
                        log.warning("Received exit message, but I don't know a node with id $nodeId")
                        return@readAllAndExecuteAsync
                    }

                    log.info("Received exit message: from node ${node.name} (${node.identifier})")

                    Utils.writeMessageHeader(socket, ClusterConfigs.PKH_PONG, 0L)
                }
            }

            else -> {
                log.warning("Received cluster message with unknown type $type")
                socket.close()
            }
        }
    }

    private fun initMasterNode(listenIp: String, listenPort: Int) {
        selfNode.address = listenIp + ":" + listenPort
        selfNode.dataAddress = listenIp
        selfNode.dataPort = CowherdConfiguration.getListenPort()
        selfNode.cmdPort = listenPort

        Cowherd.createClasspathScanner()
                .matchClassesWithAnnotation(CowherdClusterMainClass::class.java) {
                    localClasspathList.mainClassName = it.name
                }
                .scan()

        cmdConnection = vertx.createNetServer(NetServerOptions()
                .setHost(listenIp)
                .setPort(listenPort))

        cmdConnection.connectHandler { s ->
            s.handler(handleMessageHeader(s))
            s.exceptionHandler(handleMessageConnectionException(s))

            s.closeHandler { _ ->
                var node: NodeInfo? = null

                nodes.entries.removeIf { (_, n) ->
                    if (n.cmdConnection == s) {
                        node = n
                        true
                    } else {
                        false
                    }
                }

                if (node != null) {
                    log.info("Node ${node!!.name} (${node!!.identifier}) has closed connection.")
                }
            }
        }

        cmdConnection.listen()

        dataConnectionContext = vertx.orCreateContext

        dataConnection = vertx.createHttpClient(HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setSsl(false)
                .setUseAlpn(false)
                .setKeepAlive(true))
    }

    private fun redirectRequestToNodes(info: RequestDelegationInfo): CompletableFuture<RequestDelegationInfo> {
        if (!ClusterConfigs.shouldRedirectUri(info.request.absoluteURI())) {
            log.fine("We should not redirect ${info.request.absoluteURI()}.")
            info.isDelegated = false
            info.tag = "MASTER"
            return CompletableFuture.completedFuture(info)
        }

        val minLoadNode = nodes.values
                .filter { it.ready }
                .minBy { it.load }

        if (minLoadNode == null) {
            log.fine("We have no slave nodes to elect.")
            info.tag = "MASTER"
            return CompletableFuture.completedFuture(info)
        }

        if (minLoadNode.load >= selfNode.load) {
            log.fine("Master is the most easy node with load ${selfNode.load}")
            info.tag = "MASTER"
            return CompletableFuture.completedFuture(info)
        }

        val f = CompletableFuture<RequestDelegationInfo>()

        log.fine("Elected node with minimal load ${minLoadNode.load}: ${minLoadNode.name} (${minLoadNode.identifier})")

        val req = info.request
        info.isDelegated = true

        req.pause()

        dataConnectionContext.runOnContext {
            val nodeReq = dataConnection.request(req.method(), minLoadNode.dataPort, minLoadNode.dataAddress, req.uri())
                    .apply {
                        for ((k, v) in req.headers()) {
                            this.putHeader(k, v)
                        }
                    }

            val startTime = System.currentTimeMillis()

            nodeReq.handler { nodeResp ->
                val resp = req.response()

                resp.statusCode = nodeResp.statusCode()
                resp.statusMessage = nodeResp.statusMessage()
                resp.isChunked = nodeResp.getHeader("Transfer-Encoding")?.equals("chunked") ?: false

                for ((k, v) in nodeResp.headers()) {
                    resp.putHeader(k, v)
                }

                nodeResp.handler { resp.write(it) }

                nodeResp.endHandler {
                    resp.end()

                    val endTime = System.currentTimeMillis()
                    val reqTime = endTime - startTime

                    log.info("Redirect ${req.uri()} to node ${minLoadNode.name} (${minLoadNode.identifier}): ${reqTime}ms")

                    minLoadNode.appendRequestTime(reqTime)

                    f.complete(info)
                }

                nodeResp.exceptionHandler {
                    log.log(Level.WARNING, "An exception occured when reading response from node ${minLoadNode.name} " +
                            "(${minLoadNode.identifier})", it)

                    removeNode(minLoadNode)

                    f.completeExceptionally(it)
                }
            }

            nodeReq.exceptionHandler {
                val msg = "An exception occured when processing request to node ${minLoadNode.name} " +
                        "(${minLoadNode.identifier})"

                log.log(Level.WARNING, msg, it)

                removeNode(minLoadNode)

                f.completeExceptionally(Exception(msg, it))
            }

            req.handler { nodeReq.write(it) }

            req.endHandler { nodeReq.end() }

            req.exceptionHandler {
                val msg = "An exception occured when sending request to node ${minLoadNode.name} " +
                        "(${minLoadNode.identifier})"

                log.log(Level.WARNING, msg, it)

                removeNode(minLoadNode)

                f.completeExceptionally(Exception(msg, it))
            }

            req.resume()
        }

        return f
    }

    private fun masterRequestDone(info: RequestDoneInfo) {
        selfNode.appendRequestTime(info.time)
    }

    private fun removeNode(node: NodeInfo) {
        nodes.values.remove(node)

        log.info("Removed node ${node.name} (${node.identifier})")
    }

    private fun updateNode(socket: NetSocket, data: JSONObject): NodeInfo {
        val nodeId = data.getString("identifier")
        val node: NodeInfo

        if (nodes.containsKey(nodeId)) {
            node = nodes[nodeId]!!
            node.updateInfoFromJSONObject(data)
        } else {
            node = NodeInfo.fromJSONObject(socket, data)
            node.ready = false
            node.updateAddressFromConnection()
            node.dataAddress = socket.remoteAddress().host()

            nodes[node.identifier] = node
        }

        return node
    }

    private fun queueReportClasspathListToNode(node: NodeInfo) {
        nodeSyncQueue.offer(node)

        if (nodeSyncQueue.size <= 1) {
            reportClasspathListToNode(node)
        }
    }

    private fun reportClasspathListToNode(node: NodeInfo) {
        node.ready = false

        if (localClasspathList.isEmpty()) {
            loadClasspathList()
        }

        val data = JSON.toJSONString(localClasspathList).toByteArray()

        Utils.writeMessageHeader(node.cmdConnection!!, ClusterConfigs.PKH_CLASSPATH_LIST, data.size.toLong())
                .write(Buffer.buffer(data))

        log.info("Reported classpath to slave node ${node.name} (${node.address}).")
    }
}