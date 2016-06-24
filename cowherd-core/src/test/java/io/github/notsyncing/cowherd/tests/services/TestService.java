package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.annotations.*;
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpGet;
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpPost;
import io.github.notsyncing.cowherd.models.UploadFileInfo;
import io.github.notsyncing.cowherd.responses.ActionResponse;
import io.github.notsyncing.cowherd.responses.FileResponse;
import io.github.notsyncing.cowherd.responses.RedirectResponse;
import io.github.notsyncing.cowherd.responses.ViewResponse;
import io.github.notsyncing.cowherd.server.CowherdLogger;
import io.github.notsyncing.cowherd.service.CowherdService;
import io.github.notsyncing.cowherd.tests.CowherdTest;
import io.github.notsyncing.cowherd.utils.FutureUtils;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.github.notsyncing.cowherd.validators.annotations.HTMLSanitize;
import io.github.notsyncing.cowherd.validators.annotations.Length;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import org.apache.commons.io.FileUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpCookie;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

class TestModel
{
    private String text;

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }
}

@Route("/TestService")
public class TestService extends CowherdService
{
    private CowherdLogger log = getLogger();
    private Path testStoragePath;

    public TestService() throws IOException
    {
        testStoragePath = Files.createTempDirectory("cowherd-test");

        getFileStorage().registerStoragePath(TestStorageEnum.TestStorage, testStoragePath);
    }

    public Path getTestStoragePath()
    {
        return testStoragePath;
    }

    public void clear()
    {
        try {
            FileUtils.deleteDirectory(testStoragePath.toFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Exported
    @HttpGet
    public CompletableFuture<String> simpleRequest()
    {
        return CompletableFuture.completedFuture("Hello, world!");
    }

    @Exported
    @HttpPost
    public CompletableFuture<String> simplePostRequest(String __body__)
    {
        CowherdTest.testSimplePostBody = __body__;
        return CompletableFuture.completedFuture("Hello, world, post!");
    }

    @Exported
    @HttpGet
    public CompletableFuture<String> echo(String data)
    {
        return CompletableFuture.completedFuture(data);
    }

    @Exported
    @HttpGet
    public CompletableFuture<FileResponse> getFile() throws IOException
    {
        Path p = Files.createTempFile("test", ".txt");

        try (FileOutputStream fs = new FileOutputStream(p.toFile());
             PrintWriter writer = new PrintWriter(fs)
        ) {
            writer.write("Hello, world!");
        }

        return CompletableFuture.completedFuture(new FileResponse(p));
    }

    @Exported
    @HttpGet
    @Filter(TestFilter.class)
    public CompletableFuture<String> filteredSimpleRequest()
    {
        return CompletableFuture.completedFuture("Hello, world!");
    }

    @Exported
    @HttpGet
    @Filter(value = TestParameterFilter.class, parameters = {
            @FilterParameter(name = "a", value = "1"),
            @FilterParameter(name = "b", value = "2")
    })
    public CompletableFuture<String> parameterFilteredSimpleRequest()
    {
        return CompletableFuture.completedFuture("Hello, world!");
    }

    @Exported
    @HttpGet
    @Filter(TestFilter.class)
    @Filter(value = TestParameterFilter.class, parameters = {
            @FilterParameter(name = "a", value = "1"),
            @FilterParameter(name = "b", value = "2")
    })
    public CompletableFuture<String> dualFilteredSimpleRequest()
    {
        return CompletableFuture.completedFuture("Hello, world!");
    }

    @Exported
    @HttpGet
    public CompletableFuture<String> filteredSimpleRequest2()
    {
        return CompletableFuture.completedFuture("Hello, world 2!");
    }

    @Exported
    @HttpGet
    @ContentType("application/json")
    public CompletableFuture<String> typedRequest()
    {
        return CompletableFuture.completedFuture("{\"a\":1}");
    }

    @Exported
    @HttpGet
    @Route(value = "^/te.html$", entry = true, viewPath = "/te.html")
    public ViewResponse<TestModel> testTemplateEngine()
    {
        TestModel m = new TestModel();
        m.setText("Hello, world!");

        return new ViewResponse<>(m);
    }

    @Exported
    @HttpGet
    public CompletableFuture<String> validatedParameterRequest(@Length(10) String data)
    {
        return CompletableFuture.completedFuture(data);
    }

    @Exported
    @HttpGet
    public CompletableFuture<String> filteredParameterRequest(@HTMLSanitize(textOnly = true) String data)
    {
        return CompletableFuture.completedFuture(data);
    }

    @Exported
    @HttpGet
    @TestAuth
    public String authRequest()
    {
        return "AUTH!";
    }

    @Exported
    @WebSocket
    public void webSocketRequest(ServerWebSocket webSocket, int id)
    {
        webSocket.writeFinalTextFrame("Hello, " + id);

        webSocket.frameHandler(f -> {
            if (f.textData().equals("Ping")) {
                webSocket.writeFinalTextFrame("Pong");
            }
        });
    }

    @Exported
    @HttpPost
    public CompletableFuture<String> uploadRequest(UploadFileInfo file, int id)
    {
        try {
            String data = StringUtils.streamToString(new FileInputStream(file.getFile()));
            return CompletableFuture.completedFuture("id: " + id + " filename: " + file.getFilename() + " param: " +
                    file.getParameterName() + " data: " + data);
        } catch (Exception e) {
            return FutureUtils.failed(e);
        }
    }

    @Exported
    @HttpGet
    public String cookiesRequest(HttpServerRequest req, String a, String b, String c)
    {
        putCookie(req, new HttpCookie("a", a));
        putCookie(req, new HttpCookie("b", b));
        putCookie(req, new HttpCookie("c", c));

        return "done";
    }

    @Exported
    @HttpGet
    public ActionResponse redirectRequest()
    {
        return new RedirectResponse("aaa");
    }
}
