package io.github.notsyncing.cowherd.tests;

import io.github.notsyncing.cowherd.utils.RequestUtils;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

class TestParamClass
{
    public String e;
    public int f;
}

public class RequestUtilsTest
{
    private Method testMethod1;

    public RequestUtilsTest()
    {
        for (Method m : getClass().getDeclaredMethods()) {
            if (m.getName().equals("testMethod1")) {
                testMethod1 = m;
                break;
            }
        }
    }

    private void testMethod1(String a, int b, List<String> c, TestParamClass d)
    {
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
}
