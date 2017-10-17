package io.github.notsyncing.cowherd.cluster.tests

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import io.github.notsyncing.cowherd.Cowherd
import io.github.notsyncing.cowherd.cluster.ClusterConfigs
import io.github.notsyncing.cowherd.cluster.CowherdClusterMaster
import io.github.notsyncing.cowherd.cluster.Utils
import io.github.notsyncing.cowherd.cluster.enums.FileItemType
import io.github.notsyncing.cowherd.cluster.models.FileItem
import io.github.notsyncing.cowherd.cluster.models.NodeInfo
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.ArrayUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files
import java.nio.file.Path

@RunWith(VertxUnitRunner::class)
class MasterNodeTest {
    private lateinit var vertx: Vertx
    private lateinit var cowherd: Cowherd
    private lateinit var master: CowherdClusterMaster

    private val tempFiles = mutableListOf<Path>()

    @Before
    fun setUp() {
        cowherd = Cowherd()
        cowherd.start()

        master = CowherdClusterMaster()
        master.init()

        vertx = Vertx.vertx()
    }

    @After
    fun tearDown() {
        vertx.close()

        master.destroy()

        cowherd.stop().get()

        for (p in tempFiles) {
            if (Files.isDirectory(p)) {
                FileUtils.deleteDirectory(p.toFile())
            } else {
                Files.deleteIfExists(p)
            }
        }

        ClusterConfigs.reset()
    }

    @Test
    fun testMasterReceivedPing(context: TestContext) {
        TestUtils.connectTo(vertx, context, master.selfNode.cmdPort, {
            it.write(ClusterConfigs.PKH_PING)
            it.write(Buffer.buffer(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)))
        }, { it, _, done ->
            val header = ClusterConfigs.PKH_PONG.toByteArray()
            val data = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)

            Assert.assertArrayEquals(ArrayUtils.addAll(header, *data), it.bytes)

            done(null)
        })
    }

    @Test
    fun testMasterReceivedInfo(context: TestContext) {
        val node = NodeInfo("test_node", "1:2:3", 5, "127.0.0.2:8093",
                "127.0.0.2", 8094, 8095)
        val data = JSONArray().fluentAdd(node).toJSONString().toByteArray()

        TestUtils.connectTo(vertx, context, master.selfNode.cmdPort, {
            it.write(ClusterConfigs.PKH_INFO)
            it.write(Buffer.buffer(Utils.longToBytes(data.size.toLong())))
            it.write(Buffer.buffer(data))
        }, { it, s, done ->
            val header = ClusterConfigs.PKH_CLASSPATH_LIST.toByteArray()
            Assert.assertArrayEquals(header, it.bytes.copyOfRange(0, header.size))

            val length = Utils.bytesToLong(it.bytes.copyOfRange(header.size, header.size + 8))
            Assert.assertTrue(length > 0L)

            Utils.readBytes(s, length, it, header.size + 8) {
                val d = it.toString(Charsets.UTF_8)
                Assert.assertEquals(JSON.toJSONString(master.localClasspathList), d)

                Assert.assertEquals(1, master.nodes.size)

                val n = master.nodes.values.first()
                Assert.assertEquals(node.name, n.name)
                Assert.assertEquals(node.identifier, n.identifier)
                Assert.assertEquals(node.dataPort, n.dataPort)
                Assert.assertFalse(node.ready)

                done(null)
            }.exceptionally {
                done(it)
            }
        })
    }

    @Test
    fun testMasterReceivedRequestFile(context: TestContext) {
        val file = Files.createTempFile("cowherd-cluster-test-", ".txt")
        tempFiles.add(file)

        Files.newBufferedWriter(file).use {
            it.write("Hello, world!")
        }

        val data = file.toAbsolutePath().normalize().toString().toByteArray()

        TestUtils.connectTo(vertx, context, master.selfNode.cmdPort, {
            it.write(ClusterConfigs.PKH_REQUEST_FILE)
            it.write(Buffer.buffer(Utils.longToBytes(data.size.toLong())))
            it.write(Buffer.buffer(data))
        }, { it, s, done ->
            val header = ClusterConfigs.PKH_FILE.toByteArray()
            Assert.assertArrayEquals(header, it.bytes.copyOfRange(0, header.size))

            val length = Utils.bytesToLong(it.bytes.copyOfRange(header.size, header.size + 8))
            Assert.assertTrue(length > 0L)

            Utils.readBytes(s, length, it, header.size + 8) {
                val d = it.toString(Charsets.UTF_8)
                Assert.assertEquals("Hello, world!", d)

                done(null)
            }.exceptionally {
                done(it)
            }
        })
    }

    @Test
    fun testMasterReceivedRequestFileList(context: TestContext) {
        val dir = Files.createTempDirectory("cowherd-cluster-test-")
        tempFiles.add(dir)

        val f1 = dir.resolve("test1.txt")
        Files.createFile(f1)

        val f2 = dir.resolve("test2.tar.gz")
        Files.createFile(f2)

        val f3 = dir.resolve("test3.jar")
        Files.createFile(f3)

        val dir4 = dir.resolve("test4")
        Files.createDirectory(dir4)

        val f5 = dir4.resolve("test5.js")
        Files.createFile(f5)

        val data = dir.toAbsolutePath().normalize().toString().toByteArray()

        TestUtils.connectTo(vertx, context, master.selfNode.cmdPort, {
            it.write(ClusterConfigs.PKH_REQUEST_FILE_LIST)
            it.write(Buffer.buffer(Utils.longToBytes(data.size.toLong())))
            it.write(Buffer.buffer(data))
        }, { it, s, done ->
            val header = ClusterConfigs.PKH_FILE_LIST.toByteArray()
            Assert.assertArrayEquals(header, it.bytes.copyOfRange(0, header.size))

            val length = Utils.bytesToLong(it.bytes.copyOfRange(header.size, header.size + 8))
            Assert.assertTrue(length > 0L)

            Utils.readBytes(s, length, it, header.size + 8) {
                val d = it.toString(Charsets.UTF_8)
                val list = JSON.parseArray(d, FileItem::class.java)
                Assert.assertEquals(4, list.size)

                val map = list.groupBy { it.path }

                Assert.assertTrue(map.containsKey(f1.toAbsolutePath().toString()))
                Assert.assertTrue(map.containsKey(f2.toAbsolutePath().toString()))
                Assert.assertTrue(map.containsKey(f3.toAbsolutePath().toString()))
                Assert.assertTrue(map.containsKey(dir4.toAbsolutePath().toString()))

                Assert.assertEquals(FileItemType.File, map[f1.toAbsolutePath().toString()]!![0].type)
                Assert.assertEquals(FileItemType.File, map[f2.toAbsolutePath().toString()]!![0].type)
                Assert.assertEquals(FileItemType.File, map[f3.toAbsolutePath().toString()]!![0].type)
                Assert.assertEquals(FileItemType.Directory, map[dir4.toAbsolutePath().toString()]!![0].type)

                done(null)
            }.exceptionally {
                done(it)
            }
        })
    }

    @Test
    fun testMasterReceivedSynchronizeDone(context: TestContext) {
        val node = NodeInfo("test_node", "1:2:3", 5, "127.0.0.2:8093",
                "127.0.0.2", 8094, 8095)

        master.nodes.put(node.identifier, node)

        TestUtils.connectTo(vertx, context, master.selfNode.cmdPort, {
            it.write(ClusterConfigs.PKH_SYNCHRONIZE_DONE)
            it.write(Buffer.buffer(Utils.longToBytes(5L)))
            it.write("1:2:3")
        }, { it, s, done ->
            val header = ClusterConfigs.PKH_PONG.toByteArray()
            Assert.assertArrayEquals(header, it.bytes.copyOfRange(0, header.size))

            val length = Utils.bytesToLong(it.bytes.copyOfRange(header.size, header.size + 8))
            Assert.assertEquals(0L, length)

            val n = master.nodes.values.first()
            Assert.assertTrue(n.ready)

            done(null)
        })
    }

    @Test
    fun testMasterReceivedExit(context: TestContext) {
        val node = NodeInfo("test_node", "1:2:3", 5, "127.0.0.2:8093",
                "127.0.0.2", 8094, 8095)

        master.nodes.put(node.identifier, node)

        TestUtils.connectTo(vertx, context, master.selfNode.cmdPort, {
            it.write(ClusterConfigs.PKH_EXIT)
            it.write(Buffer.buffer(Utils.longToBytes(5L)))
            it.write("1:2:3")
        }, { it, s, done ->
            val header = ClusterConfigs.PKH_PONG.toByteArray()
            Assert.assertArrayEquals(header, it.bytes.copyOfRange(0, header.size))

            val length = Utils.bytesToLong(it.bytes.copyOfRange(header.size, header.size + 8))
            Assert.assertEquals(0L, length)

            Assert.assertTrue(master.nodes.isEmpty())

            done(null)
        })
    }

    @Test
    fun testMasterRedirectRequestToSlave(context: TestContext) {
        val node = NodeInfo("test_node", "1:2:3", 1, "127.0.0.1:8093",
                "127.0.0.1", 8094, 8095)
        node.ready = true

        master.nodes.put(node.name, node)

        master.selfNode.load = 10

        val async = context.async()

        val slave = vertx.createHttpServer()
                .requestHandler {
                    it.response()
                            .putHeader("Content-Length", "26")
                            .write("<html>Hello, world!</html>")
                            .end()
                }
                .listen(8094)

        vertx.createHttpClient()
                .get(8080, "127.0.0.1", "/")
                .handler {
                    it.bodyHandler {
                        context.assertEquals("<html>Hello, world!</html>", it.bytes.toString(Charsets.UTF_8))
                        context.assertNotEquals(1, node.load)

                        slave.close()
                        async.complete()
                    }
                }
                .end()
    }
}