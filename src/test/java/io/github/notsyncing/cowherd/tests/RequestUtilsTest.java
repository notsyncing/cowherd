package io.github.notsyncing.cowherd.tests;

import com.alibaba.fastjson.JSONObject;
import io.github.notsyncing.cowherd.utils.RequestUtils;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

class TestParamClass
{
    public String e;
    public int f;
}

enum TestEnum
{
    TestA,
    TestB
}

public class RequestUtilsTest
{
    private Method testMethod1;
    private Method testMethod2;

    public RequestUtilsTest()
    {
        for (Method m : getClass().getDeclaredMethods()) {
            if (m.getName().equals("testMethod1")) {
                testMethod1 = m;
            } else if (m.getName().equals("testMethod2")) {
                testMethod2 = m;
            }
        }
    }

    private void testMethod1(String a, int b, List<String> c, TestParamClass d)
    {
    }

    private void testMethod2(String a, int b, String[] c, TestParamClass d, TestEnum g)
    {
    }

    @Test
    public void testConvertParameterListToMethodParameters()
    {
        Map<String, List<String>> params = new HashMap<>();
        params.put("a", Arrays.asList("test"));
        params.put("b", Arrays.asList("2"));
        params.put("c", Arrays.asList("h", "el", "lo"));
        params.put("d.e", Arrays.asList("test2"));
        params.put("d.f", Arrays.asList("3"));
        params.put("g", Arrays.asList(String.valueOf(TestEnum.TestB.ordinal())));

        Object[] results = RequestUtils.convertParameterListToMethodParameters(testMethod2, null, params, null, null);
        assertEquals("test", results[0]);
        assertEquals(2, results[1]);
        assertArrayEquals(new String[] { "h", "el", "lo" }, ((String[])results[2]));

        TestParamClass d = (TestParamClass)results[3];
        assertNotNull(d);
        assertEquals("test2", d.e);
        assertEquals(3, d.f);

        assertEquals(TestEnum.TestB, results[4]);
    }

    @Test
    public void testConvertParameterListToMethodParametersWithJSON()
    {
        String json = "{" +
                "\"a\": \"test\"," +
                "\"b\": 2," +
                "\"c\": [\"h\", \"el\", \"lo\"]," +
                "\"d\": {" +
                "\"e\": \"test2\"," +
                "\"f\": 3" +
                "}" +
                "}";

        List<String> s = new ArrayList<>();
        s.add(json);

        Map<String, List<String>> params = new HashMap<>();
        params.put("__json__", s);

        Object[] results = RequestUtils.convertParameterListToMethodParameters(testMethod1, null, params, null, null);
        assertEquals("test", results[0]);
        assertEquals(2, results[1]);
        assertArrayEquals(new String[] { "h", "el", "lo" }, ((List<String>)results[2]).toArray(new String[0]));

        TestParamClass d = (TestParamClass)results[3];
        assertNotNull(d);
        assertEquals("test2", d.e);
        assertEquals(3, d.f);
    }

    @Test
    public void testComplexKeyToJsonObjectWithSimpleObject()
    {
        Map<String, List<String>> params = new HashMap<>();
        params.put("test.a", Arrays.asList("1"));
        params.put("test.b", Arrays.asList("2"));

        JSONObject hubObject = new JSONObject();

        params.forEach((k, v) -> RequestUtils.complexKeyToJsonObject(hubObject, k, v));

        assertEquals(1, hubObject.size());
        assertNotNull(hubObject.getJSONObject("test"));
        assertEquals(2, hubObject.getJSONObject("test").size());
        assertEquals("1", hubObject.getJSONObject("test").getString("a"));
        assertEquals("2", hubObject.getJSONObject("test").getString("b"));
    }

    @Test
    public void testComplexKeyToJsonObjectWithDeepSimpleObject()
    {
        Map<String, List<String>> params = new HashMap<>();
        params.put("test.a.c", Arrays.asList("1"));
        params.put("test.a.d", Arrays.asList("2"));
        params.put("test.b.e", Arrays.asList("3"));
        params.put("test.b.f", Arrays.asList("4"));

        JSONObject hubObject = new JSONObject();

        params.forEach((k, v) -> RequestUtils.complexKeyToJsonObject(hubObject, k, v));

        assertEquals(1, hubObject.size());
        assertNotNull(hubObject.getJSONObject("test"));
        assertEquals(2, hubObject.getJSONObject("test").size());
        assertEquals(2, hubObject.getJSONObject("test").getJSONObject("a").size());
        assertEquals("1", hubObject.getJSONObject("test").getJSONObject("a").getString("c"));
        assertEquals("2", hubObject.getJSONObject("test").getJSONObject("a").getString("d"));
        assertEquals(2, hubObject.getJSONObject("test").getJSONObject("b").size());
        assertEquals("3", hubObject.getJSONObject("test").getJSONObject("b").getString("e"));
        assertEquals("4", hubObject.getJSONObject("test").getJSONObject("b").getString("f"));
    }

    @Test
    public void testComplexKeyToJsonObjectWithArray()
    {
        Map<String, List<String>> params = new HashMap<>();
        params.put("test[]", Arrays.asList("1", "2", "3"));

        JSONObject hubObject = new JSONObject();

        params.forEach((k, v) -> RequestUtils.complexKeyToJsonObject(hubObject, k, v));

        assertEquals(1, hubObject.size());
        assertNotNull(hubObject.getJSONArray("test"));
        assertEquals(3, hubObject.getJSONArray("test").size());
        assertTrue(hubObject.getJSONArray("test").contains("1"));
        assertTrue(hubObject.getJSONArray("test").contains("2"));
        assertTrue(hubObject.getJSONArray("test").contains("3"));
    }

    @Test
    public void testComplexKeyToJsonObjectWithDeepArray()
    {
        Map<String, List<String>> params = new HashMap<>();
        params.put("test[].a", Arrays.asList("1", "2"));
        params.put("test[].b", Arrays.asList("3", "4"));
        params.put("test2.c[]", Arrays.asList("5", "6"));
        params.put("test2.d[].e", Arrays.asList("7", "8"));
        params.put("test2.d[].f", Arrays.asList("9", "0"));

        JSONObject hubObject = new JSONObject();

        params.forEach((k, v) -> RequestUtils.complexKeyToJsonObject(hubObject, k, v));

        assertEquals(2, hubObject.size());

        assertNotNull(hubObject.getJSONArray("test"));
        assertEquals(2, hubObject.getJSONArray("test").size());
        assertNotNull(hubObject.getJSONArray("test").getJSONObject(0));
        assertEquals(2, hubObject.getJSONArray("test").getJSONObject(0).size());
        assertEquals("1", hubObject.getJSONArray("test").getJSONObject(0).getString("a"));
        assertEquals("3", hubObject.getJSONArray("test").getJSONObject(0).getString("b"));
        assertNotNull(hubObject.getJSONArray("test").getJSONObject(1));
        assertEquals(2, hubObject.getJSONArray("test").getJSONObject(1).size());
        assertEquals("2", hubObject.getJSONArray("test").getJSONObject(1).getString("a"));
        assertEquals("4", hubObject.getJSONArray("test").getJSONObject(1).getString("b"));

        assertNotNull(hubObject.getJSONObject("test2"));
        assertEquals(2, hubObject.getJSONObject("test2").size());
        assertEquals(2, hubObject.getJSONObject("test2").getJSONArray("c").size());
        assertTrue(hubObject.getJSONObject("test2").getJSONArray("c").contains("5"));
        assertTrue(hubObject.getJSONObject("test2").getJSONArray("c").contains("6"));

        assertNotNull(hubObject.getJSONObject("test2").getJSONArray("d"));
        assertEquals(2, hubObject.getJSONObject("test2").getJSONArray("d").size());
        assertEquals("7", hubObject.getJSONObject("test2").getJSONArray("d").getJSONObject(0).getString("e"));
        assertEquals("9", hubObject.getJSONObject("test2").getJSONArray("d").getJSONObject(0).getString("f"));
        assertEquals("8", hubObject.getJSONObject("test2").getJSONArray("d").getJSONObject(1).getString("e"));
        assertEquals("0", hubObject.getJSONObject("test2").getJSONArray("d").getJSONObject(1).getString("f"));
    }
}
