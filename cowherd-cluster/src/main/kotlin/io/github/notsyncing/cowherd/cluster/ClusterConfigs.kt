package io.github.notsyncing.cowherd.cluster

import io.github.notsyncing.cowherd.models.RouteInfo
import io.github.notsyncing.cowherd.models.SimpleURI
import io.github.notsyncing.cowherd.routing.FastRouteMatcher
import io.github.notsyncing.cowherd.routing.RegexRouteMatcher
import io.github.notsyncing.cowherd.routing.RouteMatcher
import java.nio.file.Paths

object ClusterConfigs {
    const val PKH_PING = "PING"
    const val PKH_PONG = "PONG"
    const val PKH_REQUEST_INFO = "REQI"
    const val PKH_INFO = "INFO"
    const val PKH_CLASSPATH_LIST = "CLST"
    const val PKH_FILE = "FILE"
    const val PKH_REQUEST_FILE = "REQF"
    const val PKH_REQUEST_FILE_LIST = "RQFL"
    const val PKH_FILE_LIST = "FLST"
    const val PKH_SYNCHRONIZE_DONE = "SYND"

    var nodeRequestTimeSampleCount = 20

    var bootstrapperPath = Paths.get(".", "cowherd-cluster-bootstrapper.jar").toAbsolutePath().normalize()
    var localClasspath = Paths.get(".", "cluster_data").toAbsolutePath()
    var forceRedownloadClasspath = false

    private val defaultBlacklistJars = setOf("idea_rt.jar", "junit-rt.jar", "junit5-rt.jar", "charsets.jar",
            "cldrdata.jar", "dnsns.jar", "icedtea-sound.jar", "jaccess.jar", "java-atk-wrapper.jar",
            "localedata.jar", "nashorn.jar", "sunec.jar", "sunjce_provider.jar", "sunpkcs11.jar", "zipfs.jar",
            "jce.jar", "jsse.jar", "managment-agent.jar", "resources.jar", "rt.jar", "tools.jar")

    private val blacklistJars = defaultBlacklistJars.toMutableSet()

    fun addBlacklistJar(jarName: String) {
        blacklistJars.add(jarName)
    }

    fun isJarBlacklisted(jarName: String) = blacklistJars.contains(jarName)

    var skipSlaveBootstrapper = false
    var ignoreDirectoryInClasspath = false
    var disablePing = false

    var inheritSlaveAppStdStreams = false

    private val noRedirectPaths = mutableListOf<RouteInfo>()

    fun addNoRedirectPath(routeInfo: RouteInfo) {
        noRedirectPaths.add(routeInfo)
    }

    fun addNoRedirectPath(regexPath: Regex) {
        val info = RouteInfo()
        info.path = regexPath.pattern
        info.isFastRoute = false

        noRedirectPaths.add(info)
    }

    fun addNoRedirectPath(fastPath: String) {
        val info = RouteInfo()
        info.path = fastPath
        info.isFastRoute = true

        noRedirectPaths.add(info)
    }

    fun shouldRedirectUri(uri: String): Boolean {
        if (noRedirectPaths.isEmpty()) {
            return true
        }

        val simpleUri = SimpleURI(uri)
        val fastRouteMatcher = FastRouteMatcher(simpleUri)
        val regexRouteMatcher = RegexRouteMatcher(simpleUri)

        for (route in noRedirectPaths) {
            val matcher: RouteMatcher

            if (route.isFastRoute) {
                matcher = fastRouteMatcher
            } else {
                matcher = regexRouteMatcher
            }

            if (matcher.matchOnly(route)) {
                return false
            }
        }

        return true
    }

    fun reset() {
        nodeRequestTimeSampleCount = 20

        forceRedownloadClasspath = false

        bootstrapperPath = Paths.get(".", "cowherd-cluster-bootstrapper.jar").toAbsolutePath().normalize()
        localClasspath = Paths.get(".", "cluster_data").toAbsolutePath()

        skipSlaveBootstrapper = false
        ignoreDirectoryInClasspath = false
        disablePing = false

        inheritSlaveAppStdStreams = false

        blacklistJars.clear()
        blacklistJars.addAll(defaultBlacklistJars)

        noRedirectPaths.clear()
    }
}