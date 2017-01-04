package io.github.notsyncing.cowherd.tests;

import com.alibaba.fastjson.JSONObject;
import io.github.notsyncing.cowherd.exceptions.ValidationFailedException;
import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.utils.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.net.HttpCookie;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

class TestParamClass
{
    public String e;
    public int f;
}

class TestParamClass2
{
    public LocalDateTime d;
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
    private Method testMethod3;

    public RequestUtilsTest()
    {
        for (Method m : getClass().getDeclaredMethods()) {
            if (m.getName().equals("testMethod1")) {
                testMethod1 = m;
            } else if (m.getName().equals("testMethod2")) {
                testMethod2 = m;
            } else if (m.getName().equals("testMethod3")) {
                testMethod3 = m;
            }
        }
    }

    private void testMethod1(String a, int b, List<String> c, TestParamClass d)
    {
    }

    private void testMethod2(String a, int b, String[] c, TestParamClass d, TestEnum g, TestEnum h, boolean i,
                             boolean j, Long[] k)
    {
    }

    private void testMethod3(TestParamClass2 a)
    {
    }

    @Test
    public void testConvertParameterListToMethodParameters() throws IllegalAccessException, ValidationFailedException, InstantiationException
    {
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("a", "test"));
        params.add(new Pair<>("b", "2"));
        params.add(new Pair<>("c", "h"));
        params.add(new Pair<>("c", "el"));
        params.add(new Pair<>("c", "lo"));
        params.add(new Pair<>("d.e", "test2"));
        params.add(new Pair<>("d.f", "3"));
        params.add(new Pair<>("g", String.valueOf(TestEnum.TestB.ordinal())));
        params.add(new Pair<>("i", "false"));
        params.add(new Pair<>("j", "true"));
        params.add(new Pair<>("k", "1"));

        Object[] results = RequestUtils.convertParameterListToMethodParameters(testMethod2, null, params, null, null);
        assertEquals("test", results[0]);
        assertEquals(2, results[1]);
        assertArrayEquals(new String[] { "h", "el", "lo" }, ((String[])results[2]));

        TestParamClass d = (TestParamClass)results[3];
        assertNotNull(d);
        assertEquals("test2", d.e);
        assertEquals(3, d.f);

        assertEquals(TestEnum.TestB, results[4]);
        assertNull(results[5]);
        assertFalse((boolean)results[6]);
        assertTrue((boolean)results[7]);

        assertArrayEquals(new Long[] { 1L }, (Long[])results[8]);
    }

    @Test
    public void testConvertParameterListToMethodParametersWithJSON() throws IllegalAccessException, ValidationFailedException, InstantiationException
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

        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("__json__", json));

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
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("test.a", "1"));
        params.add(new Pair<>("test.b", "2"));

        JSONObject hubObject = new JSONObject();

        RequestUtils.complexKeyToJsonObject(hubObject, params);

        assertEquals(1, hubObject.size());
        assertNotNull(hubObject.getJSONObject("test"));
        assertEquals(2, hubObject.getJSONObject("test").size());
        assertEquals("1", hubObject.getJSONObject("test").getString("a"));
        assertEquals("2", hubObject.getJSONObject("test").getString("b"));
    }

    @Test
    public void testComplexKeyToJsonObjectWithDeepSimpleObject()
    {
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("test.a.c", "1"));
        params.add(new Pair<>("test.a.d", "2"));
        params.add(new Pair<>("test.b.e", "3"));
        params.add(new Pair<>("test.b.f", "4"));

        JSONObject hubObject = new JSONObject();

        RequestUtils.complexKeyToJsonObject(hubObject, params);

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
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("test[]", "1"));
        params.add(new Pair<>("test[]", "2"));
        params.add(new Pair<>("test[]", "3"));

        JSONObject hubObject = new JSONObject();

        RequestUtils.complexKeyToJsonObject(hubObject, params);

        assertEquals(1, hubObject.size());
        assertNotNull(hubObject.getJSONArray("test"));
        assertEquals(3, hubObject.getJSONArray("test").size());
        assertTrue(hubObject.getJSONArray("test").contains(1));
        assertTrue(hubObject.getJSONArray("test").contains(2));
        assertTrue(hubObject.getJSONArray("test").contains(3));
    }

    @Test
    public void testComplexKeyToJsonObjectWithDeepArray()
    {
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("test[].a", "1"));
        params.add(new Pair<>("test[].b", "3"));
        params.add(new Pair<>("test[].a", "2"));
        params.add(new Pair<>("test[].b", "4"));
        params.add(new Pair<>("test2.c[]", "5"));
        params.add(new Pair<>("test2.c[]", "6"));
        params.add(new Pair<>("test2.d[].e", "7"));
        params.add(new Pair<>("test2.d[].f", "9"));
        params.add(new Pair<>("test2.d[].e", "8"));
        params.add(new Pair<>("test2.d[].f", "0"));

        JSONObject hubObject = new JSONObject();

        RequestUtils.complexKeyToJsonObject(hubObject, params);

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
        assertTrue(hubObject.getJSONObject("test2").getJSONArray("c").contains(5));
        assertTrue(hubObject.getJSONObject("test2").getJSONArray("c").contains(6));

        assertNotNull(hubObject.getJSONObject("test2").getJSONArray("d"));
        assertEquals(2, hubObject.getJSONObject("test2").getJSONArray("d").size());
        assertEquals("7", hubObject.getJSONObject("test2").getJSONArray("d").getJSONObject(0).getString("e"));
        assertEquals("9", hubObject.getJSONObject("test2").getJSONArray("d").getJSONObject(0).getString("f"));
        assertEquals("8", hubObject.getJSONObject("test2").getJSONArray("d").getJSONObject(1).getString("e"));
        assertEquals("0", hubObject.getJSONObject("test2").getJSONArray("d").getJSONObject(1).getString("f"));
    }

    @Test
    public void testComplexKeyToJsonObjectWithNestedArray()
    {
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("test.a[].b[]", "1"));
        params.add(new Pair<>("test.a[].b[]", "2"));

        JSONObject hubObject = new JSONObject();

        RequestUtils.complexKeyToJsonObject(hubObject, params);

        assertEquals(1, hubObject.size());
        assertEquals(1, hubObject.getJSONObject("test").size());

        assertNotNull(hubObject.getJSONObject("test").getJSONArray("a"));
        assertEquals(1, hubObject.getJSONObject("test").getJSONArray("a").size());
        assertNotNull(hubObject.getJSONObject("test").getJSONArray("a").getJSONObject(0));
        assertEquals(1, hubObject.getJSONObject("test").getJSONArray("a").getJSONObject(0).size());

        assertNotNull(hubObject.getJSONObject("test").getJSONArray("a").getJSONObject(0).getJSONArray("b"));
        assertEquals(2, hubObject.getJSONObject("test").getJSONArray("a").getJSONObject(0).getJSONArray("b").size());
        assertEquals(1, (int)hubObject.getJSONObject("test").getJSONArray("a").getJSONObject(0).getJSONArray("b").getInteger(0));
        assertEquals(2, (int)hubObject.getJSONObject("test").getJSONArray("a").getJSONObject(0).getJSONArray("b").getInteger(1));
    }

    @Test
    public void testComplexKeyToJsonObjectWithComplexData()
    {
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("data.id", "1"));
        params.add(new Pair<>("data.groupType", "4"));
        params.add(new Pair<>("data.auth[].module", "2"));
        params.add(new Pair<>("data.auth[].auth[]", "3"));

        JSONObject hubObject = new JSONObject();

        RequestUtils.complexKeyToJsonObject(hubObject, params);

        assertEquals(1, hubObject.size());
        assertEquals(3, hubObject.getJSONObject("data").size());

        assertEquals(1, (int)hubObject.getJSONObject("data").getInteger("id"));
        assertEquals(4, (int)hubObject.getJSONObject("data").getInteger("groupType"));

        assertNotNull(hubObject.getJSONObject("data").getJSONArray("auth"));
        assertEquals(1, hubObject.getJSONObject("data").getJSONArray("auth").size());
        assertNotNull(hubObject.getJSONObject("data").getJSONArray("auth").getJSONObject(0));
        assertEquals(2, hubObject.getJSONObject("data").getJSONArray("auth").getJSONObject(0).size());
        assertEquals(2, (int)hubObject.getJSONObject("data").getJSONArray("auth").getJSONObject(0).getInteger("module"));

        assertNotNull(hubObject.getJSONObject("data").getJSONArray("auth").getJSONObject(0).getJSONArray("auth"));
        assertEquals(1, hubObject.getJSONObject("data").getJSONArray("auth").getJSONObject(0).getJSONArray("auth").size());
        assertEquals(3, (int)hubObject.getJSONObject("data").getJSONArray("auth").getJSONObject(0).getJSONArray("auth").getInteger(0));
    }

    @Test
    public void testEmptyStringToLocalDateTimeInJsonObject() throws IllegalAccessException, ValidationFailedException, InstantiationException
    {
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("a.d", ""));

        Object[] results = RequestUtils.convertParameterListToMethodParameters(testMethod3, null, params, null, null);
        TestParamClass2 c = (TestParamClass2) results[0];

        assertNotNull(c);
        assertNull(c.d);
    }

    @Test
    public void testParseHttpCookies()
    {
        String cookieString = "a=1; b=2; c=3";

        HttpServerRequest req = Mockito.mock(HttpServerRequest.class);
        when(req.getHeader("Cookie")).thenReturn(cookieString);

        List<HttpCookie> cookies = RequestUtils.parseHttpCookies(req);

        assertEquals(3, cookies.size());

        HttpCookie a = cookies.get(0);
        assertEquals("a", a.getName());
        assertEquals("1", a.getValue());

        HttpCookie b = cookies.get(1);
        assertEquals("b", b.getName());
        assertEquals("2", b.getValue());

        HttpCookie c = cookies.get(2);
        assertEquals("c", c.getName());
        assertEquals("3", c.getValue());
    }
}
