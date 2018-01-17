package io.github.notsyncing.cowherd.micron.tests.toys

import io.github.notsyncing.cowherd.annotations.Exported
import io.github.notsyncing.cowherd.annotations.Parameter
import io.github.notsyncing.cowherd.annotations.Route
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpGet
import io.github.notsyncing.cowherd.micron.CowherdMicron

class TestMicron : CowherdMicron("test") {
    @Exported
    @HttpGet
    @Route("/hello/:name", fastRoute = true)
    fun hello(@Parameter("name") name: String): String {
        return "Hello, $name!"
    }
}