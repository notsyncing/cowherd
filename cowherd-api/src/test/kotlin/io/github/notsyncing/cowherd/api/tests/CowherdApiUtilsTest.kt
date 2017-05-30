package io.github.notsyncing.cowherd.api.tests

import io.github.notsyncing.cowherd.api.CowherdApiUtils
import io.github.notsyncing.cowherd.api.tests.toys.SimpleClass
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class CowherdApiUtilsTest {
    @Test
    fun testStringToType_DateTimeString() {
        val expected = LocalDateTime.parse("2017-05-30T01:02:03")
        val actual = CowherdApiUtils.stringToType("2017-05-30 01:02:03", LocalDateTime::class)

        assertEquals(expected, actual)
    }

    @Test
    fun testStringToType_JSONArray() {
        val expected = intArrayOf(1, 2, 3)
        val actual = CowherdApiUtils.stringToType("[1,2,3]", IntArray::class) as IntArray

        assertArrayEquals(expected, actual)
    }

    @Test
    fun testStringToType_JSONObject() {
        val expected = SimpleClass().apply {
            a = 2
            b = "Test"
        }

        val actual = CowherdApiUtils.stringToType("{\"a\":2,\"b\":\"Test\"}", SimpleClass::class) as SimpleClass

        assertEquals(expected.a, actual.a)
        assertEquals(expected.b, actual.b)
    }
}