package io.github.notsyncing.cowherd.cluster.models

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.cowherd.cluster.ClusterConfigs
import io.vertx.core.net.NetSocket
import java.util.concurrent.ConcurrentLinkedQueue

class NodeInfo(var name: String,
               val identifier: String,
               var load: Int,
               var address: String,
               var dataAddress: String,
               var dataPort: Int,
               var cmdPort: Int,
               val cmdConnection: NetSocket? = null) {
    companion object {
        fun fromJSONObject(cmdConn: NetSocket, o: JSONObject): NodeInfo {
            return NodeInfo(o.getString("name"), o.getString("identifier"), o.getInteger("load"),
                    o.getString("address"), o.getString("dataAddress"), o.getInteger("dataPort"),
                    o.getInteger("cmdPort"), cmdConn)
        }
    }

    private var reqTimeList = ConcurrentLinkedQueue<Long>()

    var ready = false

    fun toJSONObject(): JSONObject {
        return JSONObject()
                .fluentPut("name", name)
                .fluentPut("identifier", identifier)
                .fluentPut("load", load)
                .fluentPut("address", address)
                .fluentPut("dataAddress", dataAddress)
                .fluentPut("dataPort", dataPort)
                .fluentPut("cmdPort", cmdPort)
    }

    fun updateAddressFromConnection(connection: NetSocket? = cmdConnection) {
        if (connection != null) {
            address = connection.remoteAddress().host() + ":" + connection.remoteAddress().port()
        }
    }

    fun updateInfoFromJSONObject(data: JSONObject) {
        load = data.getInteger("load")
        name = data.getString("name")
        address = data.getString("address")
        dataAddress = data.getString("dataAddress")
        dataPort = data.getInteger("dataPort")
        cmdPort = data.getInteger("cmdPort")
    }

    private fun computeLoad() {
        load = reqTimeList.average().toInt()
    }

    fun appendRequestTime(time: Long) {
        if (reqTimeList.size >= ClusterConfigs.nodeRequestTimeSampleCount) {
            reqTimeList.poll()
        }

        reqTimeList.offer(time)

        computeLoad()
    }
}