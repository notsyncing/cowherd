package io.github.notsyncing.cowherd.cluster.tests

import io.github.notsyncing.cowherd.Cowherd
import io.github.notsyncing.cowherd.cluster.ClusterConfigs
import io.github.notsyncing.cowherd.cluster.CowherdClusterMaster
import io.github.notsyncing.cowherd.cluster.tests.toys.TestMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CommonNodeTest {
    private lateinit var cowherd: Cowherd
    private lateinit var master: CowherdClusterMaster

    @Before
    fun setUp() {
        cowherd = Cowherd()
        cowherd.start()

        master = CowherdClusterMaster()
        master.init()
    }

    @After
    fun tearDown() {
        master.destroy()

        cowherd.stop().get()

        ClusterConfigs.reset()
    }

    @Test
    fun testGetClasspathList() {
        Assert.assertEquals(TestMain::class.java.name, master.localClasspathList.mainClassName)
        Assert.assertFalse(master.localClasspathList.isEmpty())
    }
}