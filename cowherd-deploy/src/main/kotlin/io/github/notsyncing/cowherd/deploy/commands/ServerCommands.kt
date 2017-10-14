package io.github.notsyncing.cowherd.deploy.commands

import com.alibaba.fastjson.JSON
import io.github.notsyncing.cowherd.deploy.Command
import io.github.notsyncing.cowherd.deploy.CowherdDeployApp
import io.github.notsyncing.cowherd.deploy.ServerOperator
import io.github.notsyncing.cowherd.deploy.configs.AppDeployConfig
import java.nio.file.Files
import java.nio.file.Paths

class ServerCommands(private val app: CowherdDeployApp) : CommandBase() {
    private var server: ServerOperator? = null

    private fun connect(host: String, port: String, username: String, password: String?) {
        if (server != null) {
            server!!.destroy()
        }

        try {
            server = ServerOperator(host, port.toInt(), username)
            server!!.initializeEnvironment(password)
            server!!.startDockerForwarding()
            server!!.startDockerClient()

            println("Server connected.")
        } catch (e: Exception) {
            e.printStackTrace()

            if (server != null) {
                try {
                    server!!.destroy()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                server = null
            }
        }
    }

    @Command
    fun connect(host: String, port: String, username: String) {
        connect(host, port, username, null)
    }

    @Command
    fun connectWithPassword(host: String, port: String, username: String, password: String) {
        connect(host, port, username, password)
    }

    @Command
    fun disconnect() {
        if (server == null) {
            return
        }

        server!!.destroy()

        println("Server disconnected.")
    }

    @Command
    fun appList() {
        server!!.getAppList().forEach(::println)
    }

    @Command
    fun exec(cmd: String) {
        val r = server!!.remoteExecute(cmd)
        println(r)
    }

    private fun getAppConfig(appConfigFileOrDir: String): AppDeployConfig {
        var p = Paths.get(appConfigFileOrDir)

        if (Files.isDirectory(p)) {
            p = p.resolve("app.json")
        }

        val o = JSON.parseObject(String(Files.readAllBytes(p)), AppDeployConfig::class.java)
        o.currDir = p.parent

        return o
    }

    @Command
    fun createApp(appConfigFileOrDir: String) {
        val appConf = getAppConfig(appConfigFileOrDir)
        server!!.createApp(appConf)

        println("App ${appConf.name} created.")
    }

    @Command
    fun updateApp(appConfigFileOrDir: String) {
        val appConf = getAppConfig(appConfigFileOrDir)
        server!!.updateApp(appConf)

        println("App ${appConf.name} updated.")
    }

    @Command
    fun updateAppWeb(appConfigFileOrDir: String) {
        val appConf = getAppConfig(appConfigFileOrDir)
        server!!.updateAppWeb(appConf)

        println("App ${appConf.name} web updated.")
    }

    @Command
    fun updateAppExceptDocker(appConfigFileOrDir: String) {
        val appConf = getAppConfig(appConfigFileOrDir)
        server!!.updateAppExceptDocker(appConf)

        println("App ${appConf.name} updated except docker.")
    }

    @Command
    fun updateAppRoot(appConfigFileOrDir: String) {
        val appConf = getAppConfig(appConfigFileOrDir)
        server!!.updateAppRoot(appConf)

        println("App ${appConf.name} root updated.")
    }

    @Command
    fun deleteApp(name: String) {
        server!!.deleteApp(name)

        println("App $name deleted.")
    }

    @Command
    fun startApp(name: String) {
        server!!.startApp(name)

        println("App $name started.")
    }

    @Command
    fun stopApp(name: String) {
        server!!.stopApp(name)

        println("App $name stopped.")
    }

    @Command
    fun restartApp(name: String) {
        server!!.restartApp(name)

        println("App $name restarted.")
    }

    @Command
    fun attachToAppConsole(name: String) {
        server!!.attachToAppConsole(name)
    }

    @Command
    fun startAppShell(name: String) {
        server!!.startAppShell(name)
    }

    @Command
    fun startAppShellWith(name: String, shell: String) {
        server!!.startAppShell(name, shell)
    }

    @Command
    fun exit() {
        try {
            if (server != null) {
                server!!.destroy()
            }

            app.stop()
        } catch (e: Exception) {
            e.printStackTrace()
            System.exit(-1)
        }
    }

    @Command
    fun backupAppData(name: String, toPath: String) {
        server!!.backupAppData(name, Paths.get(toPath))
    }
}