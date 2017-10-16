package io.github.notsyncing.cowherd.cluster.tests

import io.github.notsyncing.cowherd.Cowherd
import io.github.notsyncing.cowherd.cluster.ClusterConfigs
import io.github.notsyncing.cowherd.cluster.CowherdClusterMaster
import io.github.notsyncing.cowherd.cluster.CowherdClusterSlave
import io.vertx.core.Vertx
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files
import java.nio.file.Paths

@RunWith(VertxUnitRunner::class)
class ClusterTest {
    private lateinit var vertx: Vertx
    private lateinit var cowherd: Cowherd
    private lateinit var master: CowherdClusterMaster
    private lateinit var slave: CowherdClusterSlave

    private val results = mutableListOf<String?>()

    @Before
    fun setUp() {
        ClusterConfigs.bootstrapperPath = Paths.get(".", "..",
                "cowherd-cluster-bootstrap/build/libs/cowherd-cluster-bootstrap-0.9.9-all.jar")
                .toAbsolutePath()
                .normalize()

        if (!Files.exists(ClusterConfigs.bootstrapperPath)) {
            throw Exception("Please execute the shadowJar task of module cowherd-cluster-bootstrap before running this test!")
        }

        ClusterConfigs.localClasspath = Files.createTempDirectory("cowherd-cluster-test")
                .toAbsolutePath()
                .normalize()

        ClusterConfigs.inheritSlaveAppStdStreams = true
        ClusterConfigs.forceRedownloadClasspath = true

        cowherd = Cowherd()
        cowherd.start()

        master = CowherdClusterMaster()
        master.init()

        slave = CowherdClusterSlave()
        slave.init()

        vertx = Vertx.vertx()

        results.clear()
    }

    @After
    fun tearDown() {
        vertx.close()

        slave.destroy()
        master.destroy()

        cowherd.stop().get()

        FileUtils.deleteDirectory(ClusterConfigs.localClasspath.toFile())

        ClusterConfigs.reset()

        results.clear()
    }

    private fun sendRequestToMaster() {
        val client = vertx.createHttpClient()

        client.get(8080, "127.0.0.1", "/TestService/test")
                .exceptionHandler {
                    it.printStackTrace()

                    synchronized(results) {
                        results.add("EXCEPTION: ${it.message}")
                    }
                }
                .handler {
                    var added = false

                    it.exceptionHandler {
                        it.printStackTrace()

                        if (!added) {
                            synchronized(results) {
                                results.add("EXCEPTION: ${it.message}")
                            }
                        }
                    }

                    it.handler {
                        synchronized(results) {
                            results.add(it.bytes.toString(Charsets.UTF_8))
                            added = true
                        }
                    }
                }
                .end()
    }

    @Test
    fun testClusterWithOneSlave() {
        while ((master.nodes.isEmpty()) || (master.nodes.values.firstOrNull()?.ready != true)) {
            System.out.println("Waiting for slave nodes online...")

            Thread.sleep(2000)
        }

        for (i in 0 until 5) {
            sendRequestToMaster()
        }

        while (results.size < 5) {
            System.out.println("Waiting for all requests done...")

            Thread.sleep(1000)
        }

        val resultSet = results.toSet()

        Assert.assertEquals(2, resultSet.size)
        Assert.assertTrue(resultSet.contains("Hello, world from master!"))
        Assert.assertTrue(resultSet.contains("Hello, world from slave!"))
    }
}