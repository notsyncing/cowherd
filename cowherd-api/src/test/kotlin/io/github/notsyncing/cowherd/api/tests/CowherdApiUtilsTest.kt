package io.github.notsyncing.cowherd.api.tests

import com.alibaba.fastjson.JSON
import io.github.notsyncing.cowherd.api.CowherdApiUtils
import io.github.notsyncing.cowherd.api.MethodCallInfo
import io.github.notsyncing.cowherd.api.tests.toys.DeepClass
import io.github.notsyncing.cowherd.api.tests.toys.SimpleClass
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import kotlin.reflect.jvm.javaType

class CowherdApiUtilsTest {
    @Test
    fun testStringToType_DateTimeString() {
        val expected = LocalDateTime.parse("2017-05-30T01:02:03")
        val actual = CowherdApiUtils.stringToType("2017-05-30 01:02:03", LocalDateTime::class.java)

        assertEquals(expected, actual)
    }

    @Test
    fun testStringToType_JSONArray() {
        val expected = intArrayOf(1, 2, 3)
        val actual = CowherdApiUtils.stringToType("[1,2,3]", IntArray::class.java) as IntArray

        assertArrayEquals(expected, actual)
    }

    @Test
    fun testStringToType_JSONArray2() {
        val expected = arrayOf(1, 2, 3)
        val actual = CowherdApiUtils.stringToType("[1,2,3]", Array<Int>::class.java) as Array<Int>

        assertArrayEquals(expected, actual)
    }

    private fun t(l: ArrayList<SimpleClass>) {}

    @Test
    fun testStringToType_JSONArrayDeep() {
        val expected = arrayOf(SimpleClass().apply { a = 1; b = "Test1" },
                SimpleClass().apply { a = 2; b = "Test2" },
                SimpleClass().apply { a = 3; b = "Test3" })

        val l = this::t.parameters[0].type.javaType

        val actual = CowherdApiUtils.stringToType("[{\"a\":1,\"b\":\"Test1\"},{\"a\":2,\"b\":\"Test2\"},{\"a\":3,\"b\":\"Test3\"}]", l) as ArrayList<SimpleClass>

        assertEquals(3, actual.size)
        assertEquals(expected[0].a, actual[0].a)
        assertEquals(expected[0].b, actual[0].b)
        assertEquals(expected[1].a, actual[1].a)
        assertEquals(expected[1].b, actual[1].b)
        assertEquals(expected[2].a, actual[2].a)
        assertEquals(expected[2].b, actual[2].b)
    }

    @Test
    fun testStringToType_JSONObject() {
        val expected = SimpleClass().apply {
            a = 2
            b = "Test"
        }

        val actual = CowherdApiUtils.stringToType("{\"a\":2,\"b\":\"Test\"}", SimpleClass::class.java) as SimpleClass

        assertEquals(expected.a, actual.a)
        assertEquals(expected.b, actual.b)
    }

    @Test
    fun testStringToType_JSONObjectDeep() {
        val expected = DeepClass().apply {
            c = 1
            o = SimpleClass().apply {
                a = 2
                b = "Test"
            }
        }

        val actual = CowherdApiUtils.stringToType("{\"c\":1,\"o\":{\"a\":2,\"b\":\"Test\"}}", DeepClass::class.java) as DeepClass

        assertEquals(expected.c, actual.c)
        assertEquals(expected.o!!.a, actual.o!!.a)
        assertEquals(expected.o!!.b, actual.o!!.b)
    }

    private fun f (arr: IntArray) {}

    @Test
    fun testExpandJsonToMethodParameters_primitiveArray() {
        val methodCallInfo = MethodCallInfo(this::f)
        val obj = "{\"arr\":[1,2,3]}"
        val r = CowherdApiUtils.expandJsonToMethodParameters(methodCallInfo, JSON.parseObject(obj), null)

        assertEquals(1, r.size)
        assertArrayEquals(intArrayOf(1, 2, 3), r.values.first() as IntArray)
    }

//    @Test
//    fun testExpandJsonToMethodParameters_boxedArray() {
//        val methodCallInfo = MethodCallInfo(this::f)
//        val obj = "{\"arr\":[1,2,3]}"
//        val r = CowherdApiUtils.expandJsonToMethodParameters(methodCallInfo, JSON.parseObject(obj), null)
//
//        assertEquals(1, r.size)
//        assertArrayEquals(arrayOf(1, 2, 3), r.values.first() as Array<Int>)
//    }
}