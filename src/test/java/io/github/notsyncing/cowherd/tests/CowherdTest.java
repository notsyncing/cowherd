package io.github.notsyncing.cowherd.tests;

import io.github.notsyncing.cowherd.Cowherd;
import io.github.notsyncing.cowherd.commons.GlobalStorage;
import io.github.notsyncing.cowherd.server.FilterManager;
import io.github.notsyncing.cowherd.service.CowherdAPIService;
import io.github.notsyncing.cowherd.service.ServiceManager;
import io.github.notsyncing.cowherd.tests.services.TestFilter;
import io.github.notsyncing.cowherd.tests.services.TestGlobalFilter;
import io.github.notsyncing.cowherd.tests.services.TestRoutedFilter;
import io.github.notsyncing.cowherd.tests.services.TestService;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class CowherdTest
{
    Cowherd cowherd;
    Vertx vertx = Vertx.vertx();
    public static boolean testFilterTriggered = false;
    public static boolean testGlobalFilterTriggered = false;
    public static boolean testRoutedFilterTriggered = false;

    HttpClientRequest get(String uri)
    {
        HttpClient client = vertx.createHttpClient();
        return client.get(GlobalStorage.getListenPort(), "localhost", uri);
    }

    HttpClientRequest post(String uri)
    {
        HttpClient client = vertx.createHttpClient();
        return client.post(GlobalStorage.getListenPort(), "localhost", uri);
    }

    void resetValues()
    {
        testFilterTriggered = false;
        testGlobalFilterTriggered = false;
        testRoutedFilterTriggered = false;
    }

    @Before
    public void before()
    {
        resetValues();

        cowherd = new Cowherd();
        cowherd.start();
    }

    @After
    public void after() throws ExecutionException, InterruptedException
    {
        resetValues();

        cowherd.stop().get();
    }

    @Test
    public void testIfServicesAdded()
    {
        assertTrue(ServiceManager.isServiceClassAdded(CowherdAPIService.class));
        assertTrue(ServiceManager.isServiceClassAdded(TestService.class));
    }

    @Test
    public void testIfFiltersAdded()
    {
        assertTrue(FilterManager.isNormalFilter(TestFilter.class));
        assertFalse(FilterManager.isGlobalFilter(TestFilter.class));
        assertFalse(FilterManager.isRoutedFilter(TestFilter.class));

        assertTrue(FilterManager.isGlobalFilter(TestGlobalFilter.class));
        assertFalse(FilterManager.isNormalFilter(TestGlobalFilter.class));
        assertFalse(FilterManager.isRoutedFilter(TestGlobalFilter.class));

        assertTrue(FilterManager.isRoutedFilter(TestRoutedFilter.class));
        assertFalse(FilterManager.isNormalFilter(TestRoutedFilter.class));
        assertFalse(FilterManager.isGlobalFilter(TestRoutedFilter.class));
    }

    @Test
    public void testSimpleRequest(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/TestService/simpleRequest");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("Hello, world!", b.toString());
                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testEchoRequest(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/TestService/echo?data=test");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("test", b.toString());
                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testNonExistActionRequest(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/TestService/NON_EXISTS");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(404, resp.statusCode());
            async.complete();
        });

        req.end();
    }

    @Test
    public void testNonExistRouteRequest(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/WhatService/NON_EXISTS");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(404, resp.statusCode());
            async.complete();
        });

        req.end();
    }

    @Test
    public void testWrongHttpMethodToAction(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = post("/TestService/simpleRequest");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(403, resp.statusCode());
            async.complete();
        });

        req.end();
    }

    @Test
    public void testGetFile(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/TestService/getFile");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("Hello, world!", b.toString());
                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testSimpleRequestThroughAPIService(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/api/gateway?__service__=TestService&__action__=simpleRequest");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("Hello, world!", b.toString());
                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testFilteredSimpleRequest(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/TestService/filteredSimpleRequest");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("Hello, world!", b.toString());
                context.assertTrue(testGlobalFilterTriggered);
                context.assertTrue(testRoutedFilterTriggered);
                context.assertTrue(testFilterTriggered);

                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testFilteredSimpleRequest2(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/TestService/filteredSimpleRequest2");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("Hello, world 2!", b.toString());
                context.assertTrue(testGlobalFilterTriggered);
                context.assertTrue(testRoutedFilterTriggered);
                context.assertFalse(testFilterTriggered);

                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testTypedRequest(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/TestService/typedRequest");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());
            context.assertEquals("application/json", resp.getHeader("Content-Type"));

            resp.bodyHandler(b -> {
                context.assertEquals("{\"a\":1}", b.toString());
                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testFileRequest(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/a.txt");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());
            context.assertEquals("text/plain", resp.getHeader("Content-Type"));

            resp.bodyHandler(b -> {
                context.assertEquals("Hello", b.toString());
                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testSimpleRequestWithHomeChar(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/ANYPATH/~/TestService/simpleRequest");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("Hello, world!", b.toString());
                async.complete();
            });
        });

        req.end();
    }
}
