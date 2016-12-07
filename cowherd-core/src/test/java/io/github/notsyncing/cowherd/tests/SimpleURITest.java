package io.github.notsyncing.cowherd.tests;

import io.github.notsyncing.cowherd.models.SimpleURI;
import org.junit.Test;

import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SimpleURITest {
    @Test
    public void testNormalUrl() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com/abc.html");
        assertEquals("http", uri.getScheme());
        assertEquals("www.test.com", uri.getHost());
        assertEquals(80, uri.getPort());
        assertEquals("/abc.html", uri.getPath());
        assertNull(uri.getFragment());
        assertNull(uri.getQuery());
    }

    @Test
    public void testFullUrl() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com:876/awef/abc.html?id=234&wef=45#awef");
        assertEquals("http", uri.getScheme());
        assertEquals("www.test.com", uri.getHost());
        assertEquals(876, uri.getPort());
        assertEquals("/awef/abc.html", uri.getPath());
        assertEquals("awef", uri.getFragment());
        assertEquals("id=234&wef=45", uri.getQuery());
    }

    @Test
    public void testNormalUrlWithQueryString() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com/abc.html?id=234&ew=4");
        assertEquals("id=234&ew=4", uri.getQuery());
    }

    @Test
    public void testNormalUrlWithQueryStringAndFragment() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com/abc.html?id=324#wef");
        assertEquals("wef", uri.getFragment());
        assertEquals("id=324", uri.getQuery());
    }

    @Test
    public void testNormalUrlWithFragment() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com/#wef");
        assertEquals("wef", uri.getFragment());
    }

    @Test
    public void testUrlWithRootPath() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com/");
        assertEquals("/", uri.getPath());
    }

    @Test
    public void testUrlWithDirectQueryString() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com?id=2");
        assertEquals("www.test.com", uri.getHost());
        assertEquals("id=2", uri.getQuery());
    }

    @Test
    public void testUrlWithPort() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("http://www.test.com:423/abc.html");
        assertEquals(423, uri.getPort());
    }

    @Test
    public void testUrlWithHttps() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("https://www.test.com/");
        assertEquals("https", uri.getScheme());
    }

    @Test
    public void testUrlWithNoPath() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("https://www.test.com");
        assertEquals("www.test.com", uri.getHost());
        assertEquals("/", uri.getPath());
    }

    @Test
    public void testUrlWithNoPathAndFragment() throws URISyntaxException
    {
        SimpleURI uri = new SimpleURI("https://www.test.com#wef");
        assertEquals("www.test.com", uri.getHost());
        assertEquals("wef", uri.getFragment());
    }
}
