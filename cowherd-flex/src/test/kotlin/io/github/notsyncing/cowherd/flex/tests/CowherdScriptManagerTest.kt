package io.github.notsyncing.cowherd.flex.tests

import com.mashape.unirest.http.Unirest
import io.github.notsyncing.cowherd.Cowherd
import io.github.notsyncing.cowherd.flex.CowherdScriptManager
import org.junit.*
import org.junit.Assert.assertEquals

class CowherdScriptManagerTest {
    companion object {
        private val cowherd = Cowherd()

        @BeforeClass
        @JvmStatic
        fun classSetUp() {
            cowherd.start()
        }

        @AfterClass
        fun classTearDown() {
            cowherd.stop()
        }
    }

    @Before
    fun setUp() {
        CowherdScriptManager.init()
    }

    @After
    fun tearDown() {
        CowherdScriptManager.destroy()
        CowherdScriptManager.reset()
    }

    @Test
    fun testSimple() {
        val actual = Unirest.get("http://localhost:8080/simple")
                .asString()
                .body

        assertEquals("Hello, world!", actual)
    }

    @Test
    fun testSimpleParam() {
        val actual = Unirest.get("http://localhost:8080/simpleParam?param=aaa")
                .asString()
                .body

        assertEquals("Hello, aaa!", actual)
    }
}