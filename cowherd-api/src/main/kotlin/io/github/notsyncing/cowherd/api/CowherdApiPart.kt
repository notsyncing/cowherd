package io.github.notsyncing.cowherd.api

import io.github.notsyncing.cowherd.CowherdPart
import io.github.notsyncing.cowherd.commons.CowherdConfiguration
import io.github.notsyncing.cowherd.models.RouteInfo
import io.github.notsyncing.cowherd.service.ServiceManager
import io.vertx.core.json.JsonObject

class CowherdApiPart : CowherdPart {
    private val defConfig = JsonObject()

    init {
        defConfig.put("urlPrefix", "service")
    }

    override fun init() {
        var config = CowherdConfiguration.getRawConfiguration().getJsonObject("api")

        if (config == null) {
            config = defConfig
        }

        val apiRoute = RouteInfo()
        apiRoute.path = "^/${config.getString("urlPrefix")}/gateway/(?<path>.*?)$"
        apiRoute.domain = config.getString("domain")

        ServiceManager.addServiceClass(CowherdApiGatewayService::class.java, apiRoute)
    }

    override fun destroy() {
        CowherdApiHub.reset()
    }
}