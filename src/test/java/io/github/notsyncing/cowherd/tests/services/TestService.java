package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.annotations.*;
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpGet;
import io.github.notsyncing.cowherd.responses.FileResponse;
import io.github.notsyncing.cowherd.responses.ViewResponse;
import io.github.notsyncing.cowherd.service.CowherdService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
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
    @Exported
    @HttpGet
    public CompletableFuture<String> simpleRequest()
    {
        return CompletableFuture.completedFuture("Hello, world!");
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
    @Route(value = "/te.html", entry = true)
    public ViewResponse testTemplateEngine()
    {
        TestModel m = new TestModel();
        m.setText("Hello, world!");

        return new ViewResponse(m);
    }
}
