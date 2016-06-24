package io.github.notsyncing.cowherd.tests;

import com.alibaba.fastjson.JSON;
import io.github.notsyncing.cowherd.Cowherd;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.files.FileStorage;
import io.github.notsyncing.cowherd.models.ActionResult;
import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.server.FilterManager;
import io.github.notsyncing.cowherd.service.CowherdAPIService;
import io.github.notsyncing.cowherd.service.DependencyInjector;
import io.github.notsyncing.cowherd.service.ServiceManager;
import io.github.notsyncing.cowherd.tests.services.*;
import io.github.notsyncing.cowherd.utils.FileUtils;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

@RunWith(VertxUnitRunner.class)
public class CowherdTest
{
    private Cowherd cowherd;
    private Vertx vertx = Vertx.vertx();
    private TestService service;

    public static int testFilterEarlyTriggerCount = 0;
    public static int testGlobalFilterEarlyTriggerCount = 0;
    public static int testRoutedFilterEarlyTriggerCount = 0;

    public static int testFilterBeforeTriggerCount = 0;
    public static int testGlobalFilterBeforeTriggerCount = 0;
    public static int testRoutedFilterBeforeTriggerCount = 0;

    public static Map<String, String> testFilterParameters;

    public static List<Pair<String, String>> testFilterRequestParameters;
    public static ActionResult testFilterRequestResult;

    public static boolean testAuthenticatorTriggered = false;
    public static int testAuthenticatorTriggerCount = 0;

    public static String testSimplePostBody = null;

    private String encode(String data)
    {
        try {
            return URLEncoder.encode(data, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

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
        testSimplePostBody = null;
    }

    @Before
    public void before() throws IllegalAccessException, InvocationTargetException, InstantiationException
    {
        resetValues();

        cowherd = new Cowherd();
        cowherd.start();

        service = DependencyInjector.getComponent(TestService.class);
    }

    @After
    public void after() throws ExecutionException, InterruptedException, IOException
    {
        resetValues();

        cowherd.stop().get();
        service.clear();
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

    private void checkIfSuccessAndString(TestContext context, Async async, HttpClientRequest req, String expected)
    {
        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals(expected, b.toString());
                async.complete();
            });
        });
    }

    @Test
    public void testReadConfiguration() throws IOException
    {
        String s = FileUtils.getInternalResourceAsString("/cowherd.config");
        JsonObject o = new JsonObject(s);

        assertEquals((int) o.getInteger("listenPort"), CowherdConfiguration.getListenPort());

        List<Path> contextRoots = JSON.parseArray(o.getJsonArray("contextRoots").toString(), Path.class);
        assertArrayEquals(contextRoots.toArray(new Path[0]), CowherdConfiguration.getContextRoots());

        assertNotNull(CowherdConfiguration.getWebsocketConfig());
        assertTrue(CowherdConfiguration.getWebsocketConfig().isEnabled());
        assertEquals("/websocket", CowherdConfiguration.getWebsocketConfig().getPath());
        assertEquals(File.separator + "tmp" + File.separator + "cowherd_logs", CowherdConfiguration.getLogDir().toString());
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
    public void testSimplePostRequest(TestContext context)
    {
        Async async = context.async();
        HttpClientRequest req = post("/TestService/simplePostRequest");

        req.handler(r -> {
            r.bodyHandler(b -> {
                assertEquals("Hello, world, post!", b.toString());
            });

            r.endHandler(v -> {
                assertEquals("Test body!", testSimplePostBody);
                async.complete();
            });
        });

        req.putHeader("Content-Length", "10");
        req.write("Test body!");
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
                context.assertEquals("a", testFilterRequestParameters.get(0).getKey());
                context.assertEquals("1", testFilterRequestParameters.get(0).getValue());
                context.assertEquals("b", testFilterRequestParameters.get(1).getKey());
                context.assertEquals("2", testFilterRequestParameters.get(1).getValue());

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
    public void testFilteredParameterRequest(TestContext context)
    {
        String data = "<div>Hello</div>";

        Async async = context.async();
        HttpClientRequest req = get("/TestService/filteredParameterRequest?data=" + encode(data));
        req.exceptionHandler(context::fail);

        checkIfSuccessAndString(context, async, req, "Hello");

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

        final int[] state = {0};

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

    @Test
    public void testUploadFileRequest() throws IOException
    {
        Path tf = Files.createTempFile("cowherd-test", ".txt");
        Files.write(tf, "Hello".getBytes());

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost f = new HttpPost("http://localhost:" + CowherdConfiguration.getListenPort() + "/TestService/uploadRequest");
        MultipartEntityBuilder b = MultipartEntityBuilder.create();
        b.addTextBody("id", "5");
        b.addBinaryBody("file", tf.toFile(), ContentType.APPLICATION_OCTET_STREAM, "test.txt");
        HttpEntity e = b.build();

        f.setEntity(e);

        CloseableHttpResponse resp = httpClient.execute(f);
        HttpEntity respEntity = resp.getEntity();

        String data = StringUtils.streamToString(respEntity.getContent());
        assertEquals("id: 5 filename: test.txt param: file data: Hello", data);

        Files.deleteIfExists(tf);
    }

    @Test
    public void testCookiesRequest() throws IOException
    {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet g = new HttpGet("http://localhost:" + CowherdConfiguration.getListenPort() + "/TestService/cookiesRequest?a=1&b=2&c=3");

            try (CloseableHttpResponse resp = client.execute(g)) {
                Header[] cookieHeaders = resp.getHeaders("Set-Cookie");
                assertEquals("a=1", cookieHeaders[0].getValue());
                assertEquals("b=2", cookieHeaders[1].getValue());
                assertEquals("c=3", cookieHeaders[2].getValue());
            }
        }
    }

    @Test
    public void testAlternativeCookieHeaders() throws IOException
    {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet g = new HttpGet("http://localhost:" + CowherdConfiguration.getListenPort() + "/TestService/cookiesRequest?a=1&b=2&c=3");
            g.addHeader("Cowherd-Need-Alternative-Cookie-Headers", "true");

            try (CloseableHttpResponse resp = client.execute(g)) {
                Header[] cookieHeaders = resp.getHeaders("Cowherd-Set-Cookie");
                assertEquals("a=1", cookieHeaders[0].getValue());
                assertEquals("b=2", cookieHeaders[1].getValue());
                assertEquals("c=3", cookieHeaders[2].getValue());
            }
        }
    }

    @Test
    public void testNotAllowedAlternativeCookieHeaders() throws IOException
    {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet g = new HttpGet("http://localhost:" + CowherdConfiguration.getListenPort() + "/TestService/cookiesRequest?a=1&b=2&c=3");

            try (CloseableHttpResponse resp = client.execute(g)) {
                Header[] cookieHeaders = resp.getHeaders("Cowherd-Set-Cookie");
                assertEquals(0, cookieHeaders.length);
            }
        }
    }

    @Test
    public void testDirectFileStorageRoute(TestContext context) throws IllegalAccessException, InvocationTargetException, InstantiationException, IOException, ExecutionException, InterruptedException
    {
        String route = "^/test/images/(?<path>.*?)$";
        FileStorage storage = DependencyInjector.getComponent(FileStorage.class);
        storage.registerServerRoute(TestStorageEnum.TestStorage, route);

        Path tempFile = Files.createTempFile("test", null);
        Files.write(tempFile, "hello".getBytes("utf-8"));
        Path newFile = storage.storeFile(tempFile, TestStorageEnum.TestStorage, null, false).get();

        Async async = context.async();
        HttpClientRequest req = get("/test/images/" + newFile.getFileName());
        req.exceptionHandler(context::fail);

        req.handler(resp -> {
            context.assertEquals(200, resp.statusCode());

            resp.bodyHandler(b -> {
                context.assertEquals("hello", b.toString());
                async.complete();
            });
        });

        req.end();
    }
}
