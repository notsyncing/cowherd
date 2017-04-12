package io.github.notsyncing.cowherd.deploy.configs

import com.alibaba.fastjson.annotation.JSONField
import java.nio.file.Paths

class AppDeployConfig {
    inner class Directories {
        var root = ""
        var data: String? = null
        var web: String? = null
        var configs: String? = null
        var backup: String? = null

        val absRoot get() = currDir.resolve(root).toAbsolutePath()
        val absData get() = currDir.resolve(data).toAbsolutePath()
        val absWeb get() = currDir.resolve(web).toAbsolutePath()
        val absConfigs get() = currDir.resolve(configs).toAbsolutePath()
        val absBackup get() = currDir.resolve(backup).toAbsolutePath()
    }

    inner class Web {

    }

    var name = ""

    var dockerFile = ""
    val absDockerFile get() = currDir.resolve(dockerFile).toAbsolutePath()

    var directories = Directories()
    var webConfig: Web? = null

    @JSONField(serialize = false, deserialize = false)
    var currDir = Paths.get(".")
}