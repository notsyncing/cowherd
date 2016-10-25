package io.github.notsyncing.cowherd.api.tests

import com.nhaarman.mockito_kotlin.mock
import io.github.notsyncing.cowherd.Cowherd
import io.github.notsyncing.cowherd.api.CowherdApiGatewayService
import io.github.notsyncing.cowherd.api.CowherdApiHub
import io.github.notsyncing.cowherd.api.tests.toys.SimpleService
import io.github.notsyncing.cowherd.models.Pair
import io.github.notsyncing.cowherd.service.ServiceManager
import kotlinx.coroutines.async
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class CowherdApiGatewayServiceTest {
    private val server = Cowherd()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.stop().get()
    }

    private fun getService(): CowherdApiGatewayService {
        return ServiceManager.getServiceInstance(CowherdApiGatewayService::class.java) as CowherdApiGatewayService
    }

    private fun makeParamList(keys: Array<String> = emptyArray(), values: Array<String> = emptyArray()): List<Pair<String, String>> {
        val list = ArrayList<Pair<String, String>>()

        if (keys.isEmpty()) {
            return list
        }

        for (i in 0..keys.size - 1) {
            list.add(Pair(keys[i], values[i]))
        }

        return list
    }

    @Test
    fun testSimpleRequest() {
        CowherdApiHub.publish(SimpleService::class.java)

        async<Unit> {
            val service = getService()
            val r = await(service.gateway("${SimpleService::class.java.canonicalName}/${SimpleService::hello.name}", mock(),
                    makeParamList(), null, null))

            Assert.assertEquals("Hello, world!", r)
        }
    }
}