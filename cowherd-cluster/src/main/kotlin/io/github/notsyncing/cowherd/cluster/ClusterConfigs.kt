package io.github.notsyncing.cowherd.cluster

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
    }
}