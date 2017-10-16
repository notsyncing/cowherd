package io.github.notsyncing.cowherd.cluster.tests.toys

import io.github.notsyncing.cowherd.annotations.Exported
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpGet
import io.github.notsyncing.cowherd.service.CowherdService

class TestService : CowherdService() {
    @Exported
    @HttpGet
    fun test(): String {
        if ("slave".equals(System.getProperty("cowherd.cluster.mode"))) {
            return "Hello, world from slave!"
        } else {
            return "Hello, world from master!"
        }
    }
}