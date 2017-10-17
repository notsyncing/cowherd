package io.github.notsyncing.cowherd.cluster

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.cowherd.cluster.enums.FileItemType
import io.github.notsyncing.cowherd.cluster.models.ClasspathList
import io.github.notsyncing.cowherd.cluster.models.FileItem
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetClientOptions
import io.vertx.core.net.NetSocket
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.logging.Level
import kotlin.concurrent.thread

class CowherdClusterSlave : CowherdClusterNode() {
    private lateinit var config: JSONObject

    private lateinit var upstreamClient: NetClient
    private var upstreamConnection: NetSocket? = null
    private var process: Process? = null
    private lateinit var masterIp: String
    private var masterPort: Int = 0
    private var slaveDataPort: Int = 0

    private lateinit var keepAliveTimer: Thread
    protected var noPing = false

    private var currentDownloadPath: Path? = null
    private var currentDownloadCallback: ((Throwable?) -> Unit)? = null
    private var currentFileListCallback: ((List<FileItem>?) -> Unit)? = null

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val slave = CowherdClusterSlave()
            slave.init()
        }
    }

    private fun loadConfig(): ByteArray? {
        val configFile = Paths.get(".", "cluster-slave.json")

        if (!Files.exists(configFile)) {
            val stream = javaClass.getResourceAsStream("/cluster-slave.json")

            stream?.use {
                return IOUtils.toByteArray(stream)
            }

            return null
        }

        return Files.readAllBytes(configFile)
    }

    override fun init() {
        super.init()

        val configData = loadConfig()

        if (configData == null) {
            log.severe("Config file not found!")
            System.exit(-1)
            return
        }

        config = JSON.parseObject(configData.toString(Charsets.UTF_8))

        log.info("Slave node configuration loaded.")

        this.masterIp = config.getString("masterIp")
        this.masterPort = config.getInteger("masterPort")
        this.slaveDataPort = config.getInteger("slaveDataPort")

        selfNode.dataPort = slaveDataPort

        if (!ClusterConfigs.skipSlaveBootstrapper) {
            if (!Files.exists(ClusterConfigs.bootstrapperPath)) {
                log.severe("Bootstrapper not found at path ${ClusterConfigs.bootstrapperPath}, slave node cannot start!")
                throw RuntimeException("Bootstrapper not found at path ${ClusterConfigs.bootstrapperPath}")
            }
        }

        keepAliveTimer = thread(isDaemon = true, block = this::keepAliveLoop)

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            stopApp()
        })
    }

    override fun destroy() {
        keepAliveTimer.interrupt()
        keepAliveTimer.join(1500)

        upstreamConnection?.close()
        upstreamClient.close()

        super.destroy()
    }

    override fun handleMessage(socket: NetSocket, type: String, payloadLength: Long, currentBuffer: Buffer, bufferStartPos: Int) {
        val done = {
            socket.handler(handleMessageHeader(socket))
            socket.exceptionHandler(handleCmdConnectionException(socket))
        }

        when (type) {
            ClusterConfigs.PKH_PONG -> {
                Utils.drainBytes(socket)
            }

            ClusterConfigs.PKH_REQUEST_INFO -> {
                CompletableFuture.runAsync {
                    respondInfoToUpstream()
                }.exceptionally {
                    log.log(Level.WARNING, "An exception occured when receiving cmd", it)
                    null
                }
            }

            ClusterConfigs.PKH_CLASSPATH_LIST -> {
                Utils.readBytes(socket, payloadLength, currentBuffer, bufferStartPos) {
                    done()

                    CompletableFuture.runAsync {
                        val data = String(it)
                        val list = JSON.parseObject(data, ClasspathList::class.java)

                        log.info("Received classpath list from upstream: $data")

                        if (classpathHasChanged(list)) {
                            stopApp()

                            noPing = true

                            synchronizeClasspathWithUpstream(socket, list)
                                    .thenAccept {
                                        log.info("Classpath synchronized with upstream.")

                                        if (it) {
                                            startApp()
                                        }

                                        noPing = false
                                    }
                                    .exceptionally {
                                        noPing = false

                                        log.log(Level.WARNING, "An exception occured when synchronizing classpath", it)
                                        null
                                    }
                        } else {
                            log.info("Classpath up-to-date with upstream.")
                            reportSynchronizeDone(socket)
                        }
                    }.exceptionally {
                        log.log(Level.WARNING, "An exception occured when receiving cmd", it)
                        null
                    }
                }.exceptionally {
                    log.log(Level.WARNING, "An exception occured when receiving cmd", it)
                }
            }

            ClusterConfigs.PKH_FILE_LIST -> {
                Utils.readBytes(socket, payloadLength, currentBuffer, bufferStartPos) {
                    done()

                    CompletableFuture.runAsync {
                        val listData = String(it)
                        val fileList = JSON.parseArray(listData, FileItem::class.java)

                        if (currentFileListCallback != null) {
                            val cb = currentFileListCallback!!
                            currentFileListCallback = null

                            cb.invoke(fileList)
                        } else {
                            log.warning("Received file list $listData, but no callback specified!")
                        }
                    }.exceptionally {
                        log.log(Level.WARNING, "An exception occured when receiving cmd", it)
                        null
                    }
                }.exceptionally {
                    log.log(Level.WARNING, "An exception occured when receiving cmd", it)
                }
            }

            ClusterConfigs.PKH_FILE -> {
                Files.createDirectories(currentDownloadPath!!.parent)

                val stream = Files.newOutputStream(currentDownloadPath!!)

                Utils.pipeBytes(socket, stream, payloadLength, currentBuffer, bufferStartPos) {
                    done()

                    if (it != null) {
                        log.log(Level.WARNING, "An exception occured when storing file to $currentDownloadPath", it)
                    }

                    stream.close()

                    if (currentDownloadCallback != null) {
                        val cb = currentDownloadCallback!!
                        currentDownloadCallback = null

                        CompletableFuture.runAsync {
                            cb.invoke(it)
                        }
                    } else {
                        log.warning("Downloaded file to $currentDownloadPath, but no callback specified!")
                    }
                }.exceptionally {
                    log.log(Level.WARNING, "An exception occured when storing file to $currentDownloadPath", it)
                }
            }

            else -> {
                log.warning("Received cluster message with unknown type $type")
                socket.close()
            }
        }
    }

    private fun handleCmdConnectionException(socket: NetSocket): (Throwable) -> Unit = {
        log.log(Level.WARNING, "An exception occured in data connection to ${socket.remoteAddress()}", it)
    }

    fun respondInfoToUpstream() {
        val data = JSONArray()
        data.add(selfNode.toJSONObject())

        val payload = data.toJSONString()
        val d = payload.toByteArray()

        Utils.writeMessageHeader(upstreamConnection!!, ClusterConfigs.PKH_INFO, d.size.toLong())
                .write(Buffer.buffer(d))

        log.info("Reported node info to upstream.")
    }

    private fun downloadFileFromNode(socket: NetSocket, remotePath: String, localPath: Path): CompletableFuture<Unit> {
        val f = CompletableFuture<Unit>()

        currentDownloadPath = localPath
        currentDownloadCallback = {
            if (it == null) {
                log.info("Downloaded file $remotePath to $localPath from node ${socket.remoteAddress()}")
                f.complete(Unit)
            } else
                f.completeExceptionally(it)
        }

        val reqData = remotePath.toByteArray()

        Utils.writeMessageHeader(socket, ClusterConfigs.PKH_REQUEST_FILE, reqData.size.toLong())
                .write(Buffer.buffer(reqData))

        return f
    }

    private fun getFileListFromNode(socket: NetSocket, remotePath: String): CompletableFuture<List<FileItem>> {
        val f = CompletableFuture<List<FileItem>>()

        currentFileListCallback = {
            if (it == null)
                f.completeExceptionally(Exception("Failed to get file list of $remotePath from node ${socket.remoteAddress()}"))
            else
                f.complete(it)
        }

        val reqData = remotePath.toByteArray()

        Utils.writeMessageHeader(socket, ClusterConfigs.PKH_REQUEST_FILE_LIST, reqData.size.toLong())
                .write(Buffer.buffer(reqData))

        return f
    }

    private fun downloadDirectoryFromNode(socket: NetSocket, remotePath: String, localPath: Path): CompletableFuture<Unit> = future {
        log.info("Downloading directory $remotePath from upstream to $localPath")

        val list = getFileListFromNode(socket, remotePath).await()

        log.info("Got file list of remote directory $remotePath: ${JSON.toJSONString(list)}")

        for (item in list) {
            if (item.type == FileItemType.File) {
                val fileName = Paths.get(item.path).fileName.toString()
                downloadFileFromNode(socket, item.path, localPath.resolve(fileName)).await()
            } else if (item.type == FileItemType.Directory) {
                val dirName = Paths.get(item.path).fileName.toString()
                val dir = localPath.resolve(dirName)
                val storeTo = Files.createDirectories(dir)

                downloadDirectoryFromNode(socket, item.path, storeTo).await()
            } else {
                log.warning("Unsupported type ${item.type} (${item.path}) when downloading directory from node " +
                        "${socket.remoteAddress()}, it will be skipped.")
            }
        }
    }

    private fun classpathHasChanged(upstreamClasspathList: ClasspathList): Boolean {
        val upstreamMap = upstreamClasspathList.list.groupBy { it.checksum }
                .mapValues { it.value.first() }

        val localMap = localClasspathList.list.groupBy { it.checksum }
                .mapValues { it.value.first() }

        for ((checksum, item) in localMap) {
            if (item.type == FileItemType.Directory) {
                log.warning("Upstream has directory (${item.path}) in classpath list. Directories will always be fully re-downloaded.")
                return true
            }

            if (!upstreamMap.containsKey(checksum)) {
                return true
            }
        }

        for ((checksum, item) in upstreamMap) {
            if ((item.type == FileItemType.Directory) || (localMap.containsKey(checksum))) {
                continue
            }

            return true
        }

        return false
    }

    private fun synchronizeClasspathWithUpstream(socket: NetSocket, upstreamClasspathList: ClasspathList) = future {
        localClasspathList.mainClassName = upstreamClasspathList.mainClassName

        val upstreamMap = upstreamClasspathList.list.groupBy { it.checksum }
                .mapValues { it.value.first() }

        val localMap = localClasspathList.list.groupBy { it.checksum }
                .mapValues { it.value.first() }

        var anythingChanged = false

        for ((checksum, item) in localMap) {
            val itemPath = Paths.get(item.path)
            val localItemPath = ClusterConfigs.localClasspath.resolve(itemPath.fileName.toString())

            if (item.type == FileItemType.Directory) {
                log.warning("We have directory (${item.path}) in classpath list. Directories will always be fully re-downloaded.")
                FileUtils.deleteDirectory(localItemPath.toFile())
            }

            if (!upstreamMap.containsKey(checksum)) {
                anythingChanged = true
                Files.deleteIfExists(localItemPath)
            }
        }

        for ((checksum, item) in upstreamMap) {
            val filename = Paths.get(item.path).fileName.toString()
            val storeTo = ClusterConfigs.localClasspath.resolve(filename)

            if (item.type == FileItemType.Directory) {
                anythingChanged = true
                val realStoreTo = storeTo.parent.resolve(filename + "_" + System.currentTimeMillis())
                downloadDirectoryFromNode(socket, item.path, realStoreTo).await()
                continue
            }

            if ((!ClusterConfigs.forceRedownloadClasspath) && (localMap.containsKey(checksum))) {
                continue
            }

            anythingChanged = true
            downloadFileFromNode(socket, item.path, storeTo).await()
        }

        anythingChanged
    }

    private fun reportSynchronizeDone(socket: NetSocket) {
        val data = selfNode.identifier.toByteArray()

        Utils.writeMessageHeader(socket, ClusterConfigs.PKH_SYNCHRONIZE_DONE, data.size.toLong())
                .write(Buffer.buffer(data))
    }

    private fun connectToUpstream(host: String, port: Int): CompletableFuture<Unit> {
        val f = CompletableFuture<Unit>()

        log.info("Connecting to upstream $host:$port ...")

        upstreamClient = vertx.createNetClient(NetClientOptions()
                .setTcpNoDelay(true))

        upstreamClient.connect(port, host) {
            if (it.failed()) {
                log.log(Level.WARNING, "Failed to connect to upstream $host:$port", it)
                f.completeExceptionally(it.cause())
                return@connect
            }

            log.info("Connected to upstream $host:$port")

            upstreamConnection = it.result()

            selfNode.updateAddressFromConnection(upstreamConnection)
            selfNode.dataAddress = upstreamConnection!!.localAddress().host()
            selfNode.cmdPort = upstreamConnection!!.localAddress().port()

            upstreamConnection!!.handler(handleMessageHeader(upstreamConnection!!))

            upstreamConnection!!.exceptionHandler {
                log.log(Level.WARNING, "An exception occured in upstream connection to $host:$port", it)
            }

            upstreamConnection!!.closeHandler {
                upstreamConnection = null

                log.info("Upstream disconnected.")
            }

            f.complete(Unit)
        }

        return f
    }

    private fun keepAliveLoop() {
        while (true) {
            if (upstreamConnection == null) {
                connectToUpstream(masterIp, masterPort).get()
                respondInfoToUpstream()

                Thread.sleep(200)
                continue
            }

            if ((!ClusterConfigs.disablePing) && (!handlingMessage) && (!noPing)) {
                Utils.writeMessageHeader(upstreamConnection!!, ClusterConfigs.PKH_PING, 0L)
            }

            Thread.sleep(10000)
        }
    }

    private fun reportExit() {
        Utils.writeMessage(upstreamConnection!!, ClusterConfigs.PKH_EXIT, selfNode.identifier)
    }

    private fun startApp() {
        if (process != null) {
            log.severe("App already running: " + process)
            return
        }

        val readyFile = Paths.get("/tmp/cowherd-cluster-ready-" + System.currentTimeMillis())
        Files.deleteIfExists(readyFile)

        val javaHome = System.getProperty("java.home")

        process = ProcessBuilder()
                .command(javaHome + "/bin/java", "-Dcowherd.listenPort=$slaveDataPort",
                        "-Dcowherd.cluster.mode=slave", "-Dcowherd.cluster.readyFile=$readyFile",
                        "-jar", ClusterConfigs.bootstrapperPath.toString(),
                        ClusterConfigs.localClasspath.toString(), localClasspathList.mainClassName)
                .apply {
                    if (ClusterConfigs.inheritSlaveAppStdStreams) {
                        this.inheritIO()
                    }
                }
                .start()

        thread(isDaemon = true) {
            while (!Files.exists(readyFile)) {
                Thread.sleep(1000)

                if (process == null) {
                    break
                }
            }

            reportSynchronizeDone(upstreamConnection!!)
        }

        thread(isDaemon = true) {
            val r = process!!.waitFor()

            log.info("App exited with retcode $r")

            Files.deleteIfExists(readyFile)

            process = null

            reportExit()
        }

        log.info("Slave node started app: " + process)
    }

    private fun stopApp() {
        if (process == null) {
            log.warning("App already stopped!")
            return
        }

        try {
            process!!.destroy()
        } catch (e: Exception) {
            log.log(Level.WARNING, "An exception occured when stopping process " + process + ", " +
                    "will force to stop it!", e)
            process!!.destroyForcibly()
        }

        process = null

        log.info("Slave node stopped app.")
    }
}