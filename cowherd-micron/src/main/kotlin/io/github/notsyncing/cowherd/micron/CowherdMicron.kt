package io.github.notsyncing.cowherd.micron

import io.github.notsyncing.cowherd.Cowherd
import io.github.notsyncing.cowherd.commons.CowherdConfiguration
import io.github.notsyncing.cowherd.files.FileStorage
import io.github.notsyncing.cowherd.micron.config.ConfigProvider
import io.github.notsyncing.cowherd.micron.config.FileConfig
import io.github.notsyncing.cowherd.micron.enums.DeployMode
import io.github.notsyncing.cowherd.server.CowherdLogger
import io.github.notsyncing.cowherd.service.ServiceManager
import io.github.notsyncing.krafting.core.Injector
import io.github.notsyncing.krafting.core.kraftingGlobalInjector
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

abstract class CowherdMicron(private val configName: String) {
    companion object {
        var useGlobalInjector = true

        private val devMachines = HashSet<String>()

        var deployMode = DeployMode.Development

        lateinit var instance: CowherdMicron
    }

    private val cowherd: Cowherd
    private var host: String = "<UNKNOWN>"
    private var env: String = "<UNKNOWN>"

    protected val vertx: Vertx get() = cowherd.vertx
    protected val fileStorage: FileStorage
    protected val config: ConfigProvider = FileConfig()

    val injector = if (useGlobalInjector) kraftingGlobalInjector else Injector()

    private val log = Logger.getLogger(javaClass.simpleName)

    init {
        instance = this

        CowherdLogger.loggerConfigChanged()

        log.info("Working directory ${Paths.get(".").toAbsolutePath()}, starting...")

        val cowherdInternalConfigs = Cowherd.Configs().apply {
            skipClasspathScanning = true
        }

        cowherd = Cowherd(cowherdInternalConfigs)

        fileStorage = FileStorage(vertx)
    }

    protected fun addDevelopmentMachine(hostname: String) {
        devMachines.add(hostname)
    }

    private fun loadConfig() {
        host = InetAddress.getLocalHost().hostName

        if (devMachines.contains(host)) {
            env = "dev"

            log.info("Current machine is a develop machine.")

            deployMode = DeployMode.Development
        } else if (Files.exists(Paths.get("test.env"))) {
            env = "test"

            log.info("Detected test.env at ${Paths.get("test.env").toAbsolutePath()}")

            deployMode = DeployMode.Test
        } else {
            env = "production"

            deployMode = DeployMode.Production
        }

        if (!config.load(configName, env)) {
            log.warning("Failed to load configuration for $env")
        }

        val conf = config.get()?.getJSONObject("cowherd")

        if (conf != null) {
            CowherdConfiguration.fromConfig(JsonObject(conf.toJSONString()))
        }
    }

    fun start(args: Array<String> = emptyArray()) {
        loadConfig()

        log.info("Running on $host, config $configName, mode $env, working dir ${Paths.get(".").toAbsolutePath()}")

        beforeStart()

        cowherd.start()

        registerRoutes()

        log.info("Micron $this has started.")

        afterStart()
    }

    fun stop(): CompletableFuture<Unit> {
        beforeStop()

        return cowherd.stop()
                .thenApply {
                    afterStop()
                }
    }

    private fun registerRoutes() {
        val routeCount = ServiceManager.addServiceInstance(this, null)

        log.info("Registered $routeCount routes in micron $this")
    }

    private fun registerComponents() {
        injector.registerSingletonAs<CowherdMicron>(this)
        injector.registerSingletonAs<Vertx>(vertx)
        injector.registerSingletonAs<FileStorage>(fileStorage)
        injector.registerSingletonAs<ConfigProvider>(config)
    }

    open fun beforeStart() {
        registerComponents()
    }

    open fun afterStart() {}

    open fun beforeStop() {}
    open fun afterStop() {}
}