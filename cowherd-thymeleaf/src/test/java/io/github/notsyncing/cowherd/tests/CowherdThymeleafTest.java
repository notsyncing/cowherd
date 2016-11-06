package io.github.notsyncing.cowherd.tests;

import com.alibaba.fastjson.JSON;
import io.github.notsyncing.cowherd.Cowherd;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.files.FileStorage;
import io.github.notsyncing.cowherd.models.ActionResult;
import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.server.FilterManager;
import io.github.notsyncing.cowherd.service.CowherdAPIService;
import io.github.notsyncing.cowherd.service.CowherdDependencyInjector;
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
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

@RunWith(VertxUnitRunner.class)
public class CowherdThymeleafTest
{
    private Cowherd cowherd;
    private Vertx vertx = Vertx.vertx();

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

    @Before
    public void before() throws IllegalAccessException, InvocationTargetException, InstantiationException
    {
        cowherd = new Cowherd();
        cowherd.start();
    }

    @After
    public void after() throws ExecutionException, InterruptedException, IOException
    {
        cowherd.stop().get();
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
}
