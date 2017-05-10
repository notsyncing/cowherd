package io.github.notsyncing.cowherd.api.tests

import com.mashape.unirest.http.Unirest
import io.github.notsyncing.cowherd.Cowherd
import io.github.notsyncing.cowherd.api.ApiExecutor
import io.github.notsyncing.cowherd.api.CowherdApiGatewayService
import io.github.notsyncing.cowherd.api.CowherdApiHub
import io.github.notsyncing.cowherd.api.tests.toys.SimpleConstructedService
import io.github.notsyncing.cowherd.api.tests.toys.SimpleService
import io.github.notsyncing.cowherd.models.Pair
import io.github.notsyncing.cowherd.service.ServiceManager
import io.vertx.core.http.HttpServerRequest
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KCallable

class CowherdApiGatewayServiceTest {
    private val server = Cowherd()

    @Before
    fun setUp() {
        CowherdApiHub.reset()

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

        future {
            val service = getService()
            val r = service.gateway("${SimpleService::class.java.name}/${SimpleService::hello.name}", null,
                    makeParamList(), null, null).await()

            Assert.assertEquals("Hello, world!", r)
        }.get()
    }

    @Test
    fun testServiceRevoked() {
        CowherdApiHub.publish(SimpleService::class.java)
        CowherdApiHub.revoke(SimpleService::class.java)

        future {
            val service = getService()

            try {
                val r = service.gateway("${SimpleService::class.java.name}/${SimpleService::hello.name}", null,
                        makeParamList(), null, null).await()
                Assert.assertTrue(false)
            } catch (e: Exception) {
                Assert.assertTrue(e is IllegalArgumentException)
            }
        }.get()
    }

    @Test
    fun testServiceNotPublished() {
        future {
            val service = getService()

            try {
                val r = service.gateway("${SimpleService::class.java.name}/${SimpleService::hello.name}", null,
                        makeParamList(), null, null).await()
                Assert.assertTrue(false)
            } catch (e: Exception) {
                Assert.assertTrue(e is IllegalArgumentException)
            }
        }.get()
    }

    @Test
    fun testSimpleRequestThroughNetwork() {
        CowherdApiHub.publish(SimpleService::class.java)

        val resp = Unirest.get("http://localhost:8080/service/gateway/${SimpleService::class.java.name}/${SimpleService::hello.name}")
                .asString()

        Assert.assertEquals("Hello, world!", resp.body)
    }

    @Test
    fun testActionWithParameter() {
        CowherdApiHub.publish(SimpleService::class.java)

        future {
            val service = getService()
            val r = service.gateway("${SimpleService::class.java.name}/${SimpleService::helloTo.name}", null,
                    makeParamList(arrayOf("json"), arrayOf("{\"who\":\"everyone\"}")), null, null).await()

            Assert.assertEquals("Hello, everyone!", r)
        }.get()
    }

    @Test
    fun testApiExecutor() {
        CowherdApiHub.publish(SimpleConstructedService::class.java, object : ApiExecutor() {
            override fun getDefaultMethod(): KCallable<*> {
                return SimpleConstructedService::class.constructors.first()
            }

            override fun execute(method: KCallable<*>, args: MutableList<Any?>, sessionIdentifier: String?, request: HttpServerRequest?): CompletableFuture<Any?> {
                return CompletableFuture.completedFuture((method.call(*args.toTypedArray()) as SimpleConstructedService).execute())
            }
        })

        future {
            val service = getService()
            val r = service.gateway(SimpleConstructedService::class.java.name, null,
                    makeParamList(arrayOf("json"), arrayOf("{\"who\":\"const\"}")), null, null).await()

            Assert.assertEquals("Hello, new const!", r)
        }.get()
    }

    @Test
    fun testNewSessionThroughNetwork() {
        CowherdApiHub.publish(SimpleService::class.java)

        val resp = Unirest.get("http://localhost:8080/service/gateway/new_session")
                .asString()

        Assert.assertEquals(36, resp.body.length)
    }
}