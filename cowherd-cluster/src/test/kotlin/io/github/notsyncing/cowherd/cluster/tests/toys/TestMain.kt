package io.github.notsyncing.cowherd.cluster.tests.toys

import io.github.notsyncing.cowherd.Cowherd
import io.github.notsyncing.cowherd.cluster.CowherdClusterMainClass
import java.nio.file.Files
import java.nio.file.Paths

@CowherdClusterMainClass
class TestMain {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val cowherd = Cowherd()
            cowherd.start()

            val readyFile = System.getProperty("cowherd.cluster.readyFile")

            if (readyFile != null) {
                Thread.sleep(1000)

                Files.createFile(Paths.get(readyFile))
            }
        }
    }
}