package io.github.notsyncing.cowherd.api.tests

import io.github.notsyncing.cowherd.Cowherd
import org.junit.Before

class CowherdApiGatewayServiceTest {
    private val server = Cowherd()

    @Before
    fun setUp() {
        server.start()
    }

    fun tearDown() {
        server.stop().get()
    }
}