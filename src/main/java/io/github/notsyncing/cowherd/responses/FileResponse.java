package io.github.notsyncing.cowherd.responses;

import io.github.notsyncing.cowherd.utils.FileUtils;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.http.HttpServerResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class FileResponse implements ActionResponse
{
    Path file;
    InputStream stream;
    String contentType;

    public FileResponse()
    {

    }

    public FileResponse(Path file)
    {
        this.file = file;
    }

    public FileResponse(InputStream stream, String contentType)
    {
        this.stream = stream;
        this.contentType = contentType;
    }

    public Path getFile()
    {
        return file;
    }

    public void setFile(Path file)
    {
        this.file = file;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture writeToResponse(HttpServerResponse resp) throws IOException
    {
        CompletableFuture future = new CompletableFuture();

        if (file != null) {
            stream = Files.newInputStream(file);
            contentType = Files.probeContentType(file);

            resp.putHeader("Last-Modified",
                    StringUtils.dateToHttpDateString(new Date(Files.getLastModifiedTime(file).toMillis())));
        }

        if (stream != null) {
            resp.putHeader("Content-Type", contentType);
            resp.putHeader("Content-Length", String.valueOf(stream.available()));
            FileUtils.pumpInputStreamToWriteStream(stream, resp);
        } else {
            future.completeExceptionally(new IOException("Invalid file response!"));
        }

        return future;
    }
}