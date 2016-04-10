package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.annotations.ContentType;
import io.github.notsyncing.cowherd.annotations.Exported;
import io.github.notsyncing.cowherd.annotations.Filter;
import io.github.notsyncing.cowherd.annotations.Route;
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpGet;
import io.github.notsyncing.cowherd.responses.FileResponse;
import io.github.notsyncing.cowherd.service.CowherdService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

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
}
