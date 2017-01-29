package io.github.notsyncing.cowherd.server_renderer

import io.github.notsyncing.cowherd.CowherdPart
import io.github.notsyncing.cowherd.commons.CowherdConfiguration
import io.github.notsyncing.cowherd.models.RouteInfo
import io.github.notsyncing.cowherd.service.ServiceManager
import io.vertx.core.json.JsonObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit



class CowherdServerRendererPart : CowherdPart {
    private val defConfig = JsonObject()
    private lateinit var config: JsonObject
    private var phantomProcess: Process? = null
    private var phantomTmp: Path? = null

    init {
        defConfig.put("urlPrefix", "se")
        defConfig.put("phantom", JsonObject()
                .put("bin", "/usr/bin/phantomjs")
                .put("port", 46317))

        //System.setProperty("ui4j.headless", "true")
    }

    override fun init() {
        var config = CowherdConfiguration.getRawConfiguration().getJsonObject("serverRenderer")

        if (config == null) {
            config = defConfig
        }

        this.config = config

        startPhantomJs()

        val serverRendererRoute = RouteInfo()
        serverRendererRoute.path = "/${config.getString("urlPrefix")}"
        serverRendererRoute.domain = config.getString("domain")
        serverRendererRoute.isFastRoute = true

        ServiceManager.addServiceClass(ServerRendererService::class.java, serverRendererRoute)
    }

    override fun destroy() {
        stopPhantomJs()
    }

    private fun startPhantomJs() {
        val conf = config.getJsonObject("phantom")
        val binPath = Paths.get(conf.getString("bin"))
        val port = conf.getInteger("port")

        if (!Files.exists(binPath)) {
            println("Warning: PhantomJS not found at $binPath, please install it!")
            return
        }

        phantomTmp = Files.createTempDirectory("cowherd-server-renderer-")
        val js = phantomTmp!!.resolve("renderer.js")
        exportResource("/cowherd-server-renderer/renderer.js", js)

        phantomProcess = ProcessBuilder()
                .command(binPath.toString(), js.toAbsolutePath().toString())
                .inheritIO()
                .start()

        Connector.port = port

        if (!Connector.ping(3, 2000)) {
            println("Warning: Timeout waiting for renderer!")
        } else {
            println("PhantomJS renderer started.")
        }
    }

    private fun stopPhantomJs() {
        if (phantomProcess != null) {
            Connector.exit()

            phantomProcess!!.waitFor(3, TimeUnit.SECONDS)
            phantomProcess!!.destroyForcibly()
        }

        if ((phantomTmp != null) && (Files.exists(phantomTmp!!))) {
            deleteFolder(phantomTmp!!)
        }
    }

    fun exportResource(resourceName: String, toFile: Path) {
        var stream: InputStream? = null
        var resStreamOut: OutputStream? = null

        try {
            stream = this.javaClass.getResourceAsStream(resourceName)
            if (stream == null) {
                throw IOException("Cannot get resource \"$resourceName\" from Jar file.")
            }

            resStreamOut = Files.newOutputStream(toFile)
            stream.copyTo(resStreamOut)
        } catch (ex: Exception) {
            throw ex
        } finally {
            stream!!.close()
            resStreamOut!!.close()
        }
    }

    fun deleteFolder(folder: Path) {
        Files.walkFileTree(folder, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            @Throws(IOException::class)
            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (exc != null) {
                    throw exc
                }
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })
    }
}