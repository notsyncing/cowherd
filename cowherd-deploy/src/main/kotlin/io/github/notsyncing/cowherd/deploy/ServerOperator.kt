package io.github.notsyncing.cowherd.deploy

import com.alibaba.fastjson.JSON
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.command.AttachContainerResultCallback
import com.github.dockerjava.core.command.BuildImageResultCallback
import com.github.dockerjava.core.command.ExecStartResultCallback
import com.github.fommil.ssh.SshRsaCrypto
import freemarker.template.Configuration
import freemarker.template.Template
import io.github.notsyncing.cowherd.deploy.configs.AppConfig
import io.github.notsyncing.cowherd.deploy.configs.AppDeployConfig
import io.github.notsyncing.cowherd.deploy.configs.NginxSiteConfig
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.client.subsystem.sftp.SftpClient
import org.apache.sshd.common.subsystem.sftp.SftpConstants
import org.apache.sshd.common.subsystem.sftp.SftpException
import java.io.Closeable
import java.io.IOException
import java.io.StringReader
import java.lang.System.err
import java.lang.System.out
import java.net.ConnectException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.rmi.RemoteException
import java.security.KeyPair
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

class ServerOperator(private val host: String, private val port: Int, private val username: String) {
    companion object {
        val DOCKER_FORWARD_PORT = 3983
    }

    private val ssh = SshClient.setUpDefaultClient()
    private lateinit var session: ClientSession
    private lateinit var sftp: SftpClient
    private var dockerSocketForwardingThread: Thread? = null
    private var dockerSocketForwarder: Process? = null

    private val dockerConfig: DockerClientConfig
    private lateinit var docker: DockerClient

    init {
        dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:$DOCKER_FORWARD_PORT")
                .withDockerTlsVerify(false)
                .build()
    }

    private fun loadLocalSshKey(): KeyPair {
        val rsa = SshRsaCrypto()
        val pubKey = rsa.readPublicKey(rsa.slurpPublicKey(String(Files.readAllBytes(Paths.get(System.getProperty("user.home"), ".ssh/id_rsa.pub")))))
        val privKey = rsa.readPrivateKey(rsa.slurpPrivateKey(String(Files.readAllBytes(Paths.get(System.getProperty("user.home"), ".ssh/id_rsa")))))

        return KeyPair(pubKey, privKey)
    }

    private fun checkForDockerInstallation(): Boolean {
        try {
            val s = session.executeRemoteCommand("docker -v")
            println("Server: docker installed: $s")
            return true
        } catch (e: RemoteException) {
            println("Server: docker not installed.")
            return false
        }
    }

    private fun installDocker() {
        // TODO: Support different distributions with lsb_release
        try {
            session.executeRemoteCommand("apt-get -y install docker.io")
        } catch (e: Exception) {
            throw Exception("Failed to install docker on remote server!", e)
        }
    }

    private fun makeServerDirectories(path: String) {
        val parts = path.split("/")
        var currPath = ""

        for (p in parts) {
            if (currPath.endsWith("/")) {
                currPath += p
            } else {
                currPath += "/$p"
            }

            try {
                if (!keepSftp().stat(currPath).isDirectory) {
                    keepSftp().mkdir(currPath)
                }
            } catch (e: SftpException) {
                if (e.status == SftpConstants.SSH_FX_NO_SUCH_FILE) {
                    keepSftp().mkdir(currPath)
                } else {
                    throw e
                }
            }
        }
    }

    private fun hasServerDirectory(path: String): Boolean {
        try {
            return keepSftp().stat(path).isDirectory
        } catch (e: SftpException) {
            if (e.status == SftpConstants.SSH_FX_NO_SUCH_FILE) {
                return false
            }

            throw e
        }
    }

    fun initializeEnvironment() {
        println("Initializing server environment...")

        ssh.start()
        session = ssh.connect(username, host, port).apply { this.await() }.session
        session.addPublicKeyIdentity(loadLocalSshKey())

        if (!session.auth().apply { this.await() }.isSuccess) {
            throw ConnectException("Auth failed with local ssh key to server $host port $port username $username")
        }

        if (!checkForDockerInstallation()) {
            installDocker()
        }

        sftp = session.createSftpClient()
        makeServerDirectories("/data")

        println("Server: environment ready.")
    }

    private fun keepSftp(maxCount: Int = 5): SftpClient {
        var counter = 0

        while (!sftp.isOpen) {
            println("SFTP client closed, retrying to connect...")

            sftp = session.createSftpClient()
            counter++

            if (counter >= maxCount) {
                println("Failed to connect to SFTP server after $counter retries, give up.")
                break
            }
        }

        return sftp
    }

    fun destroy() {
        println("Stopping...")

        stopDockerClient()
        stopDockerForwarding()

        if (sftp.isOpen) {
            sftp.close()
        }

        session.close()
        ssh.stop()

        println("Stopped.")
    }

    fun startDockerForwarding() {
        if (dockerSocketForwardingThread != null) {
            return
        }

        dockerSocketForwardingThread = thread(isDaemon = true) {
            if (dockerSocketForwarder != null) {
                println("A docker socket already forwarded!")
                dockerSocketForwardingThread = null
                return@thread
            }

            dockerSocketForwarder = ProcessBuilder()
                    .command("ssh", "-L$DOCKER_FORWARD_PORT:/var/run/docker.sock", "$username@$host", "-p$port")
                    .start()

            println("Docker socket forwarding started.")

            val r = dockerSocketForwarder!!.waitFor()
            dockerSocketForwarder = null

            println("Docker socket forwarding ended: $r")

            dockerSocketForwardingThread = null
        }
    }

    fun stopDockerForwarding() {
        if ((dockerSocketForwardingThread == null) || (dockerSocketForwarder == null)) {
            return
        }

        dockerSocketForwarder!!.destroy()
        dockerSocketForwardingThread!!.join(3000)

        if (dockerSocketForwarder?.isAlive == true) {
            dockerSocketForwarder!!.destroyForcibly()
            dockerSocketForwardingThread!!.join()
        }
    }

    fun startDockerClient() {
        docker = DockerClientBuilder.getInstance(dockerConfig)
                .build()
    }

    fun stopDockerClient() {
        docker.close()
    }

    fun hasApp(name: String): Boolean {
        if (!hasServerDirectory("/data/$name")) {
            return false
        }

        try {
            return keepSftp().lstat("/data/$name/app.json").isRegularFile
        } catch (e: SftpException) {
            if (e.status == SftpConstants.SSH_FX_NO_SUCH_FILE) {
                return false
            }

            throw e
        }
    }

    private fun syncCopy(from: Path, to: String): Int {
        val rsync = ProcessBuilder()
                .command("rsync", "-chavzP", "--stats", "-e", "ssh -p $port", "$from/",
                        "$username@$host:$to")
                .inheritIO()
                .start()

        return rsync.waitFor()
    }

    private fun syncCopy(from: String, to: Path): Int {
        val rsync = ProcessBuilder()
                .command("rsync", "-chavzP", "--stats", "-e", "ssh -p $port", "$username@$host:$from", "$to/")
                .inheritIO()
                .start()

        return rsync.waitFor()
    }

    private fun syncApp(appConfig: AppDeployConfig, skipData: Boolean = false) {
        println("Copying root data...")

        var r = syncCopy(appConfig.directories.absRoot, "/data/${appConfig.name}")

        if (r != 0) {
            throw IOException("rsync failed with exit code $r")
        }

        println("Done.")

        if (appConfig.directories.web != null) {
            println("Copying web data...")

            r = syncCopy(appConfig.directories.absWeb, "/data/${appConfig.name}/web")

            if (r != 0) {
                throw IOException("rsync failed with exit code $r")
            }

            println("Done")
        }

        if (!skipData) {
            if (appConfig.directories.data != null) {
                println("Copying app data...")

                r = syncCopy(appConfig.directories.absData, "/data/${appConfig.name}/data")

                if (r != 0) {
                    throw IOException("rsync failed with exit code $r")
                }

                println("Done")
            }
        }

        if (appConfig.directories.configs != null) {
            println("Copying app configs...")

            r = syncCopy(appConfig.directories.absConfigs, "/data/${appConfig.name}/configs")

            if (r != 0) {
                throw IOException("rsync failed with exit code $r")
            }

            println("Done")
        }

        if (appConfig.directories.backup != null) {
            println("Copying app backup configs...")

            r = syncCopy(appConfig.directories.absBackup, "/data/${appConfig.name}/backup")

            if (r != 0) {
                throw IOException("rsync failed with exit code $r")
            }

            println("Done")
        }
    }

    private fun buildAppImage(name: String, dockerFile: Path): String {
        val imageId = docker.buildImageCmd(dockerFile.toFile())
                .withBuildArg("APP_ROOT", "/app")
                .withBuildArg("APP_WEB_ROOT", "/app/web")
                .withBuildArg("APP_DATA_ROOT", "/app/data")
                .withBuildArg("APP_CONFIG_ROOT", "/app/configs")
                .withBuildArg("APP_BACKUP_ROOT", "/app/backup")
                .exec(object : BuildImageResultCallback() {
                    override fun onNext(item: BuildResponseItem?) {
                        println("Server: docker: $item")

                        super.onNext(item)
                    }

                    override fun onError(throwable: Throwable?) {
                        super.onError(throwable)

                        println("Server: docker: error occured while building image from $dockerFile: ${throwable?.message}")
                        throwable?.printStackTrace()
                    }
                })
                .awaitImageId()

        docker.tagImageCmd(imageId, "apps", name)
                .exec()

        return imageId
    }

    private fun buildAppContainer(appName: String, imageId: String): String {
        return docker.createContainerCmd(imageId)
                .withBinds(Bind("/data/$appName", Volume("/app")),
                        Bind("/data/$appName/web", Volume("/app/web")),
                        Bind("/data/$appName/data", Volume("/app/data")),
                        Bind("/data/$appName/configs", Volume("/app/configs")),
                        Bind("/data/$appName/backup", Volume("/app/backup")))
                .withPublishAllPorts(true)
                .exec()
                .id
    }

    private fun getAppContainerPorts(containerId: String): List<Pair<Int, Int>> {
        val ports = docker.inspectContainerCmd(containerId)
                .exec()
                .networkSettings
                .ports

        if ((ports == null) || (ports.bindings.isEmpty())) {
            return emptyList()
        }

        return ports.bindings
                .map { (k, v) -> Pair(k.port, v[0].hostPortSpec.toInt()) }
                .sortedBy { (containerPort, _) -> containerPort }
    }

    private fun updateAppConfig(name: String, conf: AppConfig) {
        try {
            keepSftp().remove("/data/$name/app.json")
        } catch (e: SftpException) {
            if (e.status != SftpConstants.SSH_FX_NO_SUCH_FILE) {
                throw e
            }
        }

        keepSftp().write("/data/$name/app.json").bufferedWriter().use {
            it.write(JSON.toJSONString(conf))
        }
    }

    fun createApp(appConfig: AppDeployConfig): String? {
        val name = appConfig.name
        val dockerFile = appConfig.absDockerFile

        if (hasApp(name)) {
            println("App $name already exists.")
            return null
        }

        println("Server: creating directories for app $name")

        makeServerDirectories("/data/$name")
        makeServerDirectories("/data/$name/web")
        makeServerDirectories("/data/$name/data")
        makeServerDirectories("/data/$name/configs")
        makeServerDirectories("/data/$name/backup")

        println("Copying data to server...")

        syncApp(appConfig)

        println("Server: building app images with docker...")

        val imageId = buildAppImage(name, dockerFile)
        val containerId = buildAppContainer(name, imageId)

        val conf = AppConfig()
        conf.dockerImageId = imageId
        conf.dockerContainerId = containerId

        updateAppConfig(name, conf)

        return containerId
    }

    private fun readAppConfig(name: String): AppConfig {
        return JSON.parseObject<AppConfig>(keepSftp().read("/data/$name/app.json"), AppConfig::class.java)
    }

    private fun updateAppNginxConfig(name: String, conf: AppConfig, ports: List<Pair<Int, Int>>) {
        val nginxConfDir = "/etc/nginx/sites-available"
        val appNginxConfDir = "/data/$name/configs/nginx"

        try {
            keepSftp().stat(appNginxConfDir)
        } catch (e: SftpException) {
            if (e.status == SftpConstants.SSH_FX_NO_SUCH_FILE) {
                println("App $name has no nginx config.")

                return
            } else {
                throw e
            }
        }

        keepSftp().openDir(appNginxConfDir).use {
            keepSftp().listDir(it)
                    .filter { !it.filename.startsWith(".") }
                    .forEach {
                        var siteConfData: String? = null

                        keepSftp().read("$appNginxConfDir/${it.filename}").bufferedReader().use {
                            siteConfData = it.readText()
                        }

                        if (siteConfData == null) {
                            throw IOException("Failed to read app nginx site config file at $appNginxConfDir/${it.filename}")
                        }

                        val siteConf = Template(it.filename.substringBefore("."), StringReader(siteConfData),
                                Configuration(Configuration.VERSION_2_3_23))
                        val o = NginxSiteConfig()
                        o.backendPort = ports[0].second

                        keepSftp().write("$nginxConfDir/${it.filename}").bufferedWriter().use {
                            siteConf.process(o, it)
                        }

                        println("Updated app nginx site config ${it.filename}")
                    }
        }

        try {
            val r = session.executeRemoteCommand("nginx -t 2>&1 && nginx -s reload")
            println("Server: nginx: $r")
        } catch (e: RemoteException) {
            throw IOException("Server: error in nginx configurations!", e)
        }

        println("Server: nginx reloaded.")
    }

    fun startApp(name: String) {
        val conf = readAppConfig(name)

        docker.startContainerCmd(conf.dockerContainerId)
                .exec()

        val ports = getAppContainerPorts(conf.dockerContainerId)

        if (ports.isNotEmpty()) {
            updateAppNginxConfig(name, conf, ports)
        }
    }

    fun stopApp(name: String) {
        val conf = readAppConfig(name)

        docker.stopContainerCmd(conf.dockerContainerId)
                .exec()
    }

    fun restartApp(name: String) {
        stopApp(name)
        startApp(name)
    }

    fun attachToAppConsole(name: String) {
        val conf = readAppConfig(name)

        docker.attachContainerCmd(conf.dockerContainerId)
                .withStdErr(true)
                .withLogs(true)
                .withStdOut(true)
                .withFollowStream(true)
                .withStdIn(System.`in`)
                .exec(object : AttachContainerResultCallback() {
                    override fun onStart(stream: Closeable?) {
                        super.onStart(stream)

                        println("Attached to app $name on container ${conf.dockerContainerId}")
                    }

                    override fun onNext(item: Frame?) {
                        super.onNext(item)

                        if ((item == null) || (item.streamType == null)) {
                            return
                        }

                        when (item.streamType) {
                            StreamType.STDIN -> print("")
                            StreamType.STDOUT -> System.out.print(String(item.payload))
                            StreamType.STDERR -> System.err.print(String(item.payload))
                            StreamType.RAW -> System.out.print(String(item.payload))
                        }
                    }
                })
                .awaitCompletion()
                .close()
    }

    fun startAppShell(name: String, shell: String = "/bin/bash") {
        val conf = readAppConfig(name)

        val execId = docker.execCreateCmd(conf.dockerContainerId)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withTty(true)
                .withCmd(*shell.split(" ").toTypedArray())
                .exec()
                .id

        docker.execStartCmd(execId)
                .withStdIn(System.`in`)
                .withTty(true)
                .exec(object : ExecStartResultCallback() {
                    override fun onStart(stream: Closeable?) {
                        super.onStart(stream)

                        println("Attached to app $name on container ${conf.dockerContainerId}")
                    }

                    override fun onNext(item: Frame?) {
                        super.onNext(item)

                        if ((item == null) || (item.streamType == null)) {
                            return
                        }

                        when (item.streamType) {
                            StreamType.STDIN -> print("")
                            StreamType.STDOUT -> System.out.print(String(item.payload))
                            StreamType.STDERR -> System.err.print(String(item.payload))
                            StreamType.RAW -> System.out.print(String(item.payload))
                        }
                    }
                })
                .awaitCompletion()
                .close()
    }

    fun updateApp(appConfig: AppDeployConfig): String {
        val currConf = readAppConfig(appConfig.name)

        syncApp(appConfig, true)

        val newImageId = buildAppImage(appConfig.name, appConfig.absDockerFile)
        val newContainerId = buildAppContainer(appConfig.name, newImageId)

        currConf.dockerImageId = newImageId
        currConf.dockerContainerId = newContainerId

        updateAppConfig(appConfig.name, currConf)

        return newContainerId
    }

    fun deleteApp(name: String) {
        val currConf = readAppConfig(name)

        session.executeRemoteCommand("rm -rf /data/$name")

        docker.removeContainerCmd(currConf.dockerContainerId)
                .withForce(true)
                .exec()

        docker.removeImageCmd(currConf.dockerImageId)
                .exec()
    }

    fun getAppList(): List<String> {
        keepSftp().openDir("/data").use {
            return keepSftp().listDir(it)
                    .filter { !it.filename.startsWith(".") }
                    .map { it.filename }
        }
    }

    fun remoteExecute(cmd: String): String {
        return session.executeRemoteCommand(cmd)
    }

    fun backupAppData(name: String, toPath: Path) {
        val currConf = readAppConfig(name)

        makeServerDirectories("/data/$name/backup/staging")

        val cmdId = docker.execCreateCmd(currConf.dockerContainerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("/bin/bash", "/app/backup/backup.sh")
                .exec()
                .id

        val currTimeStr = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())

        println("Executing backup script...")

        docker.execStartCmd(cmdId)
                .withDetach(false)
                .exec(ExecStartResultCallback(out, err))
                .awaitCompletion()

        println("Preparing to copy data...")

        if (!Files.exists(toPath)) {
            Files.createDirectories(toPath)
        }

        val targetDir = toPath.resolve(currTimeStr)

        if (Files.exists(targetDir)) {
            println("ERROR: Backup target path $toPath already contains a directory named $currTimeStr")
            return
        }

        Files.createDirectories(targetDir)

        println("Copying data...")

        val r = syncCopy("/data/$name/backup/staging/*", targetDir)

        if (r == 0) {
            println("Done at ${targetDir.toAbsolutePath()}")
        } else {
            println("Backup failed: $r")
        }
    }
}