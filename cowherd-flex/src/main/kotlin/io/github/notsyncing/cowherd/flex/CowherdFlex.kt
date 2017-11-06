package io.github.notsyncing.cowherd.flex

import io.vertx.core.http.HttpMethod

object CowherdFlex {
    fun on(method: HttpMethod, route: String, action: Function<*>) {
        CowherdScriptManager.registerAction(method, route, action)
    }

    fun get(route: String, action: Function<*>) {
        on(HttpMethod.GET, route, action)
    }

    fun post(route: String, action: Function<*>) {
        on(HttpMethod.POST, route, action)
    }
}

val CF = CowherdFlex