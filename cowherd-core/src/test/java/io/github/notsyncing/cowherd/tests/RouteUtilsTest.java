package io.github.notsyncing.cowherd.tests;

import io.github.notsyncing.cowherd.models.RouteInfo;
import io.github.notsyncing.cowherd.utils.RouteUtils;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RouteUtilsTest
{
    @Test
    public void testMatchRoute() throws URISyntaxException
    {
        URI uri = new URI("http://www.test.com/a/bc/def");
        RouteInfo info = new RouteInfo();
        info.setPath("/a/bc/def");

        assertTrue(RouteUtils.matchRoute(uri, info));
    }

    @Test
    public void testMatchRouteWithNotMatch() throws URISyntaxException
    {
        URI uri = new URI("http://www.test.com/a/bc/def");
        RouteInfo info = new RouteInfo();
        info.setPath("/a/dd/aew");

        assertFalse(RouteUtils.matchRoute(uri, info));
    }

    @Test
    public void testExtractRouteParameters() throws URISyntaxException
    {
        URI uri = new URI("http://www.test.com/a/bc/def");
        RouteInfo info = new RouteInfo();
        info.setDomain("(?<domain>(.*?)).test.com");
        info.setPath("/a/bc/(?<res>(.*?))$");

        Map<String, List<String>> params = RouteUtils.extractRouteParameters(uri, info);

        assertEquals("www", params.get("domain").get(0));
        assertEquals("def", params.get("res").get(0));
    }
}
