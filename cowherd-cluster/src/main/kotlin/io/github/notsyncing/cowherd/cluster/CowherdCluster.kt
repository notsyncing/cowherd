package io.github.notsyncing.cowherd.cluster

object CowherdCluster {
    private lateinit var master: CowherdClusterMaster
    private lateinit var slave: CowherdClusterSlave

    var isMaster = System.getProperty("cowherd.cluster.mode") != "slave"

    val currentNode = if (isMaster) master else slave

    fun init() {
        if (isMaster) {
            slave = CowherdClusterSlave()
            slave.init()
        } else {
            master = CowherdClusterMaster()
            master.init()
        }
    }

    fun destroy() {
        if (isMaster) {
            master.destroy()
        } else {
            slave.destroy()
        }
    }
}