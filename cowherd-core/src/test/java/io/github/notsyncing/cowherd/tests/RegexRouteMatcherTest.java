package io.github.notsyncing.cowherd.tests;

import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.models.RouteInfo;
import io.github.notsyncing.cowherd.routing.MatchedRoute;
import io.github.notsyncing.cowherd.routing.RegexRouteMatcher;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.*;

public class RegexRouteMatcherTest
{
    @Test
    public void testMatchRoute() throws URISyntaxException
    {
        URI uri = new URI("http://www.test.com/a/bc/def");
        RouteInfo info = new RouteInfo();
        info.setPath("/a/bc/def");

        assertNotNull(new RegexRouteMatcher(uri).match(info));
    }

    @Test
    public void testMatchRouteWithMultipleSeparators() throws URISyntaxException
    {
        URI uri = new URI("http://www.test.com///a/bc/def");
        RouteInfo info = new RouteInfo();
        info.setPath("/a/bc/def");

        assertNotNull(new RegexRouteMatcher(uri).match(info));
    }

    @Test
    public void testMatchRouteWithNotMatch() throws URISyntaxException
    {
        URI uri = new URI("http://www.test.com/a/bc/def");
        RouteInfo info = new RouteInfo();
        info.setPath("/a/dd/aew");

        assertNull(new RegexRouteMatcher(uri).match(info));
    }

    @Test
    public void testExtractRouteParameters() throws URISyntaxException
    {
        URI uri = new URI("http://www.test.com/a/bc/def");
        RouteInfo info = new RouteInfo();
        info.setDomain("(?<domain>(.*?)).test.com");
        info.setPath("/a/bc/(?<res>(.*?))$");

        MatchedRoute mr = new RegexRouteMatcher(uri).match(info);
        List<Pair<String, String>> params = mr.getRouteParameters();

        assertEquals("domain", params.get(0).getKey());
        assertEquals("www", params.get(0).getValue());
        assertEquals("res", params.get(1).getKey());
        assertEquals("def", params.get(1).getValue());
    }
}
