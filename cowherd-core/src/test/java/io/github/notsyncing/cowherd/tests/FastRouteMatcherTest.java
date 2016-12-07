package io.github.notsyncing.cowherd.tests;

import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.models.RouteInfo;
import io.github.notsyncing.cowherd.models.SimpleURI;
import io.github.notsyncing.cowherd.routing.FastRouteMatcher;
import io.github.notsyncing.cowherd.routing.MatchedRoute;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.*;

public class FastRouteMatcherTest
{
    @Test
    public void testMatchRoute() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com/a/bc/def");
        RouteInfo info = new RouteInfo();
        info.setPath("/a/bc/def");

        assertNotNull(new FastRouteMatcher(uri).match(info));
    }

    @Test
    public void testMatchRouteWithMultipleSeparators() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com///a/bc/def");
        RouteInfo info = new RouteInfo();
        info.setPath("/a/bc/def");

        assertNotNull(new FastRouteMatcher(uri).match(info));
    }

    @Test
    public void testMatchRouteWithNotMatch() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com/a/bc/def");
        RouteInfo info = new RouteInfo();
        info.setPath("/a/dd/aew");

        assertNull(new FastRouteMatcher(uri).match(info));
    }

    @Test
    public void testExtractRouteParameters() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com/a/bc/def");
        RouteInfo info = new RouteInfo();
        info.setPath("/a/bc/:res");

        MatchedRoute mr = new FastRouteMatcher(uri).match(info);
        assertNotNull(mr);

        List<Pair<String, String>> params = mr.getRouteParameters();

        assertEquals("res", params.get(0).getKey());
        assertEquals("def", params.get(0).getValue());
    }

    @Test
    public void testExtractRoutePathParameters() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com/a/bc/def/ghi");
        RouteInfo info = new RouteInfo();
        info.setPath("/a/bc/*/:res");

        MatchedRoute mr = new FastRouteMatcher(uri).match(info);
        assertNotNull(mr);

        List<Pair<String, String>> params = mr.getRouteParameters();

        assertEquals("res", params.get(0).getKey());
        assertEquals("ghi", params.get(0).getValue());
    }

    @Test
    public void testExtractRouteMultiplePathParameters() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com/a/bc/def");
        RouteInfo info = new RouteInfo();
        info.setPath("/a/**:path");

        MatchedRoute mr = new FastRouteMatcher(uri).match(info);
        assertNotNull(mr);

        List<Pair<String, String>> params = mr.getRouteParameters();

        assertEquals("path", params.get(0).getKey());
        assertEquals("bc/def", params.get(0).getValue());
    }
}
