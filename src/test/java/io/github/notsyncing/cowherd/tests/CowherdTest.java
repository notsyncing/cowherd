package io.github.notsyncing.cowherd.tests;

import io.github.notsyncing.cowherd.Cowherd;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.models.ActionResult;
import io.github.notsyncing.cowherd.server.FilterManager;
import io.github.notsyncing.cowherd.service.CowherdAPIService;
import io.github.notsyncing.cowherd.service.ServiceManager;
import io.github.notsyncing.cowherd.tests.services.*;
import io.github.notsyncing.cowherd.utils.FileUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

@RunWith(VertxUnitRunner.class)
public class CowherdTest
{
    private Cowherd cowherd;
    private Vertx vertx = Vertx.vertx();

    public static int testFilterEarlyTriggerCount = 0;
    public static int testGlobalFilterEarlyTriggerCount = 0;
    public static int testRoutedFilterEarlyTriggerCount = 0;

    public static int testFilterBeforeTriggerCount = 0;
    public static int testGlobalFilterBeforeTriggerCount = 0;
    public static int testRoutedFilterBeforeTriggerCount = 0;

    public static Map<String, String> testFilterParameters;

    public static Map<String, List<String>> testFilterRequestParameters;
    public static ActionResult testFilterRequestResult;

    public static boolean testAuthenticatorTriggered = false;
    public static int testAuthenticatorTriggerCount = 0;

    private HttpClientRequest get(String uri)
    {
        HttpClient client = vertx.createHttpClient();
        return client.get(CowherdConfiguration.getListenPort(), "localhost", uri);
    }

    private HttpClientRequest post(String uri)
    {
        HttpClient client = vertx.createHttpClient();
        return client.post(CowherdConfiguration.getListenPort(), "localhost", uri);
    }

    private void resetValues()
    {
        testFilterParameters = null;
        testFilterEarlyTriggerCount = 0;
        testGlobalFilterEarlyTriggerCount = 0;
        testRoutedFilterEarlyTriggerCount = 0;
        testFilterBeforeTriggerCount = 0;
        testGlobalFilterBeforeTriggerCount = 0;
        testRoutedFilterBeforeTriggerCount = 0;
        testFilterRequestParameters = null;
        testFilterRequestResult = null;
        testAuthenticatorTriggered = false;
        testAuthenticatorTriggerCount = 0;
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

        assertFalse(FilterManager.isRoutedFilter(TestParameterFilter.class));
        assertTrue(FilterManager.isNormalFilter(TestParameterFilter.class));
        assertFalse(FilterManager.isGlobalFilter(TestParameterFilter.class));
    }

    private void checkIfSuccessAndString(TestContext context, Async async, HttpClientRequest req, String data)
    {
        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals(data, b.toString());
                async.complete();
            });
        });
    }

    @Test
    public void testReadConfiguration() throws IOException
    {
        String s = FileUtils.getInternalResourceAsString("/cowherd.config");
        JsonObject o = new JsonObject(s);

        assertEquals((int)o.getInteger("listenPort"), CowherdConfiguration.getListenPort());
        assertEquals(o.getString("contextRoot"), CowherdConfiguration.getContextRoot().toString());
        assertNotNull(CowherdConfiguration.getWebsocketConfig());
        assertTrue(CowherdConfiguration.getWebsocketConfig().isEnabled());
        assertEquals("/websocket", CowherdConfiguration.getWebsocketConfig().getPath());
        assertNotNull(CowherdConfiguration.getUserConfiguration());
        assertEquals(o.getJsonObject("user").getInteger("a"), CowherdConfiguration.getUserConfiguration().getInteger("a"));
        assertEquals(o.getJsonObject("user").getString("b"), CowherdConfiguration.getUserConfiguration().getString("b"));
    }

    @Test
    public void testSimpleRequest(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/TestService/simpleRequest");
        req.exceptionHandler(context::fail);

        checkIfSuccessAndString(context, async, req, "Hello, world!");

        req.end();
    }

    @Test
    public void testEchoRequest(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/TestService/echo?data=test");
        req.exceptionHandler(context::fail);

        checkIfSuccessAndString(context, async, req, "test");

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

        checkIfSuccessAndString(context, async, req, "Hello, world!");

        req.end();
    }

    @Test
    public void testSimpleRequestThroughAPIService(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/api/gateway?__service__=TestService&__action__=simpleRequest");
        req.exceptionHandler(context::fail);

        checkIfSuccessAndString(context, async, req, "Hello, world!");

        req.end();
    }

    @Test
    public void testFilteredSimpleRequest(TestContext context)
    {
        assertEquals(0, testFilterEarlyTriggerCount);
        assertEquals(0, testGlobalFilterEarlyTriggerCount);
        assertEquals(0, testRoutedFilterEarlyTriggerCount);

        Async async = context.async();
        HttpClientRequest req = get("/TestService/filteredSimpleRequest?a=1&b=2");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("Hello, world!", b.toString());
                assertEquals(1, testFilterEarlyTriggerCount);
                assertEquals(1, testGlobalFilterEarlyTriggerCount);
                assertEquals(1, testRoutedFilterEarlyTriggerCount);

                context.assertNotNull(testFilterRequestParameters);
                context.assertEquals(2, testFilterRequestParameters.size());
                context.assertEquals(1, testFilterRequestParameters.get("a").size());
                context.assertEquals("1", testFilterRequestParameters.get("a").get(0));
                context.assertEquals(1, testFilterRequestParameters.get("b").size());
                context.assertEquals("2", testFilterRequestParameters.get("b").get(0));

                context.assertNotNull(testFilterRequestResult);
                context.assertEquals("Hello, world!", testFilterRequestResult.getResult());

                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testFilteredSimpleRequestFailed(TestContext context)
    {
        assertEquals(0, testFilterEarlyTriggerCount);
        assertEquals(0, testGlobalFilterEarlyTriggerCount);
        assertEquals(0, testRoutedFilterEarlyTriggerCount);

        assertEquals(0, testFilterBeforeTriggerCount);
        assertEquals(0, testGlobalFilterBeforeTriggerCount);
        assertEquals(0, testRoutedFilterBeforeTriggerCount);

        Async async = context.async();
        HttpClientRequest req = get("/TestService/filteredSimpleRequest?a=1&b=2&nopass=1");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(403, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("", b.toString());
                context.assertEquals(1, testFilterEarlyTriggerCount);
                context.assertEquals(1, testGlobalFilterEarlyTriggerCount);
                context.assertEquals(1, testRoutedFilterEarlyTriggerCount);

                context.assertEquals(1, testFilterBeforeTriggerCount);
                context.assertEquals(0, testGlobalFilterBeforeTriggerCount);
                context.assertEquals(0, testRoutedFilterBeforeTriggerCount);

                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testFilteredSimpleRequestFirstFailed(TestContext context)
    {
        assertEquals(0, testFilterEarlyTriggerCount);
        assertEquals(0, testGlobalFilterEarlyTriggerCount);
        assertEquals(0, testRoutedFilterEarlyTriggerCount);

        Async async = context.async();
        HttpClientRequest req = get("/TestService/filteredSimpleRequest?a=1&b=2&nopassGlobal=1");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(403, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("", b.toString());
                context.assertEquals(1, testGlobalFilterBeforeTriggerCount);
                context.assertEquals(1, testFilterBeforeTriggerCount);
                context.assertEquals(0, testRoutedFilterBeforeTriggerCount);

                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testParameterFilteredSimpleRequest(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/TestService/parameterFilteredSimpleRequest");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("Hello, world!", b.toString());

                context.assertNotNull(testFilterParameters);
                context.assertEquals(2, testFilterParameters.size());
                context.assertEquals("1", testFilterParameters.get("a"));
                context.assertEquals("2", testFilterParameters.get("b"));

                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testDualFilteredSimpleRequest(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = get("/TestService/dualFilteredSimpleRequest");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("Hello, world!", b.toString());
                context.assertEquals(1, testFilterEarlyTriggerCount);
                context.assertEquals(1, testGlobalFilterEarlyTriggerCount);
                context.assertEquals(1, testRoutedFilterEarlyTriggerCount);

                context.assertNotNull(testFilterParameters);
                context.assertEquals(2, testFilterParameters.size());
                context.assertEquals("1", testFilterParameters.get("a"));
                context.assertEquals("2", testFilterParameters.get("b"));

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
                context.assertEquals(0, testFilterEarlyTriggerCount);
                context.assertEquals(1, testGlobalFilterEarlyTriggerCount);
                context.assertEquals(1, testRoutedFilterEarlyTriggerCount);

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

        checkIfSuccessAndString(context, async, req, "Hello, world!");

        req.end();
    }

    @Test
    public void testTemplateEngine(TestContext context)
    {
        String expected = "<!DOCTYPE html>\n" +
                "\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "\n" +
                "<head>\n" +
                "    <title>Test page</title>\n" +
                "</head>\n" +
                "\n" +
                "<body>\n" +
                "    <p>Hello, world!</p>\n" +
                "</body>\n" +
                "\n" +
                "</html>\n";

        Async async = context.async();
        HttpClientRequest req = get("/te.html");
        req.exceptionHandler(context::fail);

        checkIfSuccessAndString(context, async, req, expected);

        req.end();
    }

    @Test
    public void testEntryRequest(TestContext context)
    {
        String expected = "<!DOCTYPE html>\n" +
                "\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "\n" +
                "<head>\n" +
                "    <title>Test page</title>\n" +
                "</head>\n" +
                "\n" +
                "<body>\n" +
                "    <p>Hello, world!</p>\n" +
                "</body>\n" +
                "\n" +
                "</html>\n";

        Async async = context.async();
        HttpClientRequest req = get("/");
        req.exceptionHandler(context::fail);

        checkIfSuccessAndString(context, async, req, expected);

        req.end();
    }

    @Test
    public void testValidatedParameterRequestFailed(TestContext context)
    {
        String data = "123456789";

        Async async = context.async();
        HttpClientRequest req = get("/TestService/validatedParameterRequest?data=" + data);
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(400, resp.statusCode());
            async.complete();
        });

        req.end();
    }

    @Test
    public void testValidatedParameterRequestPassed(TestContext context)
    {
        String data = "1234567890";

        Async async = context.async();
        HttpClientRequest req = get("/TestService/validatedParameterRequest?data=" + data);
        req.exceptionHandler(context::fail);

        checkIfSuccessAndString(context, async, req, data);

        req.end();
    }

    @Test
    public void testAuthenticatedRequest(TestContext context)
    {
        assertFalse(testAuthenticatorTriggered);
        assertEquals(0, testAuthenticatorTriggerCount);

        Async async = context.async();
        HttpClientRequest req = get("/TestService/authRequest");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("AUTH!", b.toString());
                context.assertTrue(testAuthenticatorTriggered);
                context.assertEquals(1, testAuthenticatorTriggerCount);

                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testAuthenticatedRequestFailed(TestContext context)
    {
        assertFalse(testAuthenticatorTriggered);

        Async async = context.async();
        HttpClientRequest req = get("/TestService/authRequest?nopass=1");
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(403, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("", b.toString());
                context.assertTrue(testAuthenticatorTriggered);
                async.complete();
            });
        });

        req.end();
    }

    @Test
    public void testWebSocketRequest(TestContext context)
    {
        Async async = context.async();
        HttpClient client = vertx.createHttpClient();

        final int[] state = { 0 };

        client.websocket(CowherdConfiguration.getListenPort(), "localhost", "/TestService/webSocketRequest?id=2", s -> {
            s.frameHandler(f -> {
                if (state[0] == 0) {
                    context.assertEquals("Hello, 2", f.textData());
                    state[0] = 1;

                    s.writeFinalTextFrame("Ping");
                } else if (state[0] == 1) {
                    context.assertEquals("Pong", f.textData());
                    s.close();
                    client.close();
                    async.complete();
                }
            });
        });
    }
}
