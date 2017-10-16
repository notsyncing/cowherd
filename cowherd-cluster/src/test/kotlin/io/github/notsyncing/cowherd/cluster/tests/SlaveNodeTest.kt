package io.github.notsyncing.cowherd.cluster.tests

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import io.github.notsyncing.cowherd.cluster.ClusterConfigs
import io.github.notsyncing.cowherd.cluster.CowherdClusterSlave
import io.github.notsyncing.cowherd.cluster.Utils
import io.github.notsyncing.cowherd.cluster.enums.FileItemType
import io.github.notsyncing.cowherd.cluster.models.ClasspathList
import io.github.notsyncing.cowherd.cluster.models.FileItem
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeoutException

@RunWith(VertxUnitRunner::class)
class SlaveNodeTest {
    private lateinit var vertx: Vertx
    private lateinit var server: NetServer
    private lateinit var slave: CowherdClusterSlave

    private var conn: NetSocket? = null

    private val tempFiles = mutableListOf<Path>()

    @Before
    fun setUp() {
        vertx = Vertx.vertx()

        server = vertx.createNetServer()
                .connectHandler {
                    conn = it
                }
                .listen(8081)

        ClusterConfigs.skipSlaveBootstrapper = true
        ClusterConfigs.ignoreDirectoryInClasspath = true
        ClusterConfigs.disablePing = true

        slave = CowherdClusterSlave()
        slave.init()
    }

    @After
    fun tearDown() {
        if (conn != null) {
            conn!!.close()
            conn = null
        }

        server.close()
        vertx.close()

        slave.destroy()

        for (p in tempFiles) {
            if (Files.isDirectory(p)) {
                FileUtils.deleteDirectory(p.toFile())
            } else {
                Files.deleteIfExists(p)
            }
        }

        ClusterConfigs.reset()
    }

    private fun makeSureConnection() {
        var i = 5

        while (conn == null) {
            Thread.sleep(1000)
            i--

            if (i <= 0) {
                throw TimeoutException("Timeout when waiting for slave connection!")
            }
        }

        // Drain the first PKH_INFO data
        conn!!.handler {  }

        Thread.sleep(2000)
    }

    private fun makeSurePongReceived(buf: Buffer) {
        val header = ClusterConfigs.PKH_PONG.toByteArray()
        Assert.assertArrayEquals(header, buf.bytes.copyOfRange(0, header.size))

        val length = Utils.bytesToLong(buf.bytes.copyOfRange(header.size, header.size + 8))
        Assert.assertTrue(length == 0L)
    }

    @Test
    fun testSlaveReceivedRequestInfo(context: TestContext) {
        makeSureConnection()

        Utils.writeMessageHeader(conn!!, ClusterConfigs.PKH_REQUEST_INFO, 0L)

        val async = context.async()

        conn!!.handler {
            try {
                val header = ClusterConfigs.PKH_INFO.toByteArray()
                Assert.assertArrayEquals(header, it.bytes.copyOfRange(0, header.size))

                val length = Utils.bytesToLong(it.bytes.copyOfRange(header.size, header.size + 8))
                Assert.assertTrue(length > 0L)

                Utils.readBytes(conn!!, length, it, header.size + 8) {
                    val d = it.toString(Charsets.UTF_8)
                    val expected = JSONArray().fluentAdd(slave.selfNode.toJSONObject())
                            .toJSONString()

                    Assert.assertEquals(expected, d)

                    async.complete()
                }.exceptionally {
                    context.fail(it)
                }
            } catch (e: Throwable) {
                context.fail(e)
            }
        }
    }

    @Test
    fun testSlaveReceivedClasspathListWithNothingChanged(context: TestContext) {
        val classpathData = JSON.toJSONString(slave.localClasspathList).toByteArray()

        makeSureConnection()

        Utils.writeMessageHeader(conn!!, ClusterConfigs.PKH_CLASSPATH_LIST, classpathData.size.toLong())
                .write(Buffer.buffer(classpathData))

        val async = context.async()

        conn!!.handler {
            try {
                val header = ClusterConfigs.PKH_SYNCHRONIZE_DONE.toByteArray()
                Assert.assertArrayEquals(header, it.bytes.copyOfRange(0, header.size))

                val length = Utils.bytesToLong(it.bytes.copyOfRange(header.size, header.size + 8))
                Assert.assertTrue(length == 0L)

                async.complete()
            } catch (e: Throwable) {
                context.fail(e)
            }
        }
    }

    @Test
    fun testSlaveReceivedClasspathList(context: TestContext) {
        val cl = ClasspathList()
        cl.add(FileItem("/a/1.jar", FileItemType.File, "1"))
        cl.add(FileItem("/a/2.jar", FileItemType.File, "2"))
        cl.add(FileItem("/b/3.jar", FileItemType.File, "3"))

        val classpathData = JSON.toJSONString(cl).toByteArray()

        makeSureConnection()

        Utils.writeMessageHeader(conn!!, ClusterConfigs.PKH_CLASSPATH_LIST, classpathData.size.toLong())
                .write(Buffer.buffer(classpathData))

        val async = context.async()

        conn!!.handler {
            try {
                val header = ClusterConfigs.PKH_REQUEST_FILE.toByteArray()
                Assert.assertArrayEquals(header, it.bytes.copyOfRange(0, header.size))

                val length = Utils.bytesToLong(it.bytes.copyOfRange(header.size, header.size + 8))
                Assert.assertTrue(length > 0L)

                Utils.readBytes(conn!!, length, it, header.size + 8) {
                    val d = it.toString(Charsets.UTF_8)

                    Assert.assertTrue(setOf("/a/1.jar", "/a/2.jar", "/a/3.jar").contains(d))

                    async.complete()
                }.exceptionally {
                    context.fail(it)
                }
            } catch (e: Throwable) {
                context.fail(e)
            }
        }
    }

    @Test
    fun testSlaveNodeReceivedFileList(context: TestContext) {
        val list = mutableListOf<FileItem>()
        list.add(FileItem("a.txt", FileItemType.File, "1"))
        list.add(FileItem("b.jar", FileItemType.File, "2"))
        list.add(FileItem("c.js", FileItemType.File, "3"))

        val data = JSON.toJSONString(list).toByteArray()

        val async = context.async()

        slave.javaClass.getDeclaredField("currentFileListCallback")
                .apply { this.isAccessible = true }
                .set(slave, { l: List<FileItem> ->
                    try {
                        Assert.assertEquals(JSON.toJSONString(list), JSON.toJSONString(l))

                        async.complete()
                    } catch (e: Throwable) {
                        context.fail(e)
                    }
                })

        makeSureConnection()

        Utils.writeMessageHeader(conn!!, ClusterConfigs.PKH_FILE_LIST, data.size.toLong())
                .write(Buffer.buffer(data))
    }

    @Test
    fun testSlaveNodeReceivedFile(context: TestContext) {
        val data = "Hello, world!".toByteArray()
        val file = Files.createTempFile("cowherd-cluster-test", ".tmp")

        tempFiles.add(file)

        val async = context.async()

        slave.javaClass.getDeclaredField("currentDownloadPath")
                .apply { this.isAccessible = true }
                .set(slave, file)

        slave.javaClass.getDeclaredField("currentDownloadCallback")
                .apply { this.isAccessible = true }
                .set(slave, { ex: Throwable? ->
                    if (ex != null) {
                        context.fail(ex)
                        return@set
                    }

                    try {
                        Assert.assertEquals("Hello, world!", String(Files.readAllBytes(file), Charsets.UTF_8))

                        async.complete()
                    } catch (e: Throwable) {
                        context.fail(e)
                    }
                })

        makeSureConnection()

        Utils.writeMessageHeader(conn!!, ClusterConfigs.PKH_FILE, data.size.toLong())
                .write(Buffer.buffer(data))
    }
}