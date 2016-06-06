package io.github.notsyncing.cowherd.responses;

import io.github.notsyncing.cowherd.models.ActionContext;
import io.github.notsyncing.cowherd.utils.FileUtils;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.http.HttpServerResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * 服务方法返回的文件响应，用于向客户端发送一个文件
 */
public class FileResponse implements ActionResponse
{
    private Path file;
    private InputStream stream;
    private String contentType;

    public FileResponse()
    {

    }

    /**
     * 实例化文件响应对象
     * @param file 要发送的文件
     */
    public FileResponse(Path file)
    {
        this.file = file;
    }

    /**
     * 实例化文件响应对象
     * @param stream 要发送的输入流
     * @param contentType 响应内容类型
     */
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
    public CompletableFuture writeToResponse(ActionContext context) throws IOException
    {
        HttpServerResponse resp = context.getRequest().response();
        CompletableFuture future = new CompletableFuture();

        if (file != null) {
            stream = Files.newInputStream(file);
            contentType = Files.probeContentType(file);

            resp.putHeader("Last-Modified",
                    StringUtils.dateToHttpDateString(new Date(Files.getLastModifiedTime(file).toMillis())));
        }

        if (stream != null) {
            if (contentType == null) {
                contentType = "text/plain";
            }

            resp.putHeader("Content-Type", contentType);
            resp.putHeader("Content-Length", String.valueOf(stream.available()));
            FileUtils.pumpInputStreamToWriteStream(stream, resp);
            future.complete(null);
        } else {
            future.completeExceptionally(new IOException("Invalid file response!"));
        }

        return future;
    }
}