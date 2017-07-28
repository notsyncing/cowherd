package io.github.notsyncing.cowherd.responses;

import io.github.notsyncing.cowherd.models.ActionContext;
import io.github.notsyncing.cowherd.models.RangeHeaderInfo;
import io.github.notsyncing.cowherd.utils.FileUtils;
import io.github.notsyncing.cowherd.utils.RequestUtils;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 服务方法返回的文件响应，用于向客户端发送一个文件
 */
public class FileResponse implements ActionResponse
{
    private Path file;
    private Path scope;
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
     * 实例化文件响应对象，并检查该文件是否超出指定的路径范围
     * @param file 要发送的文件
     * @param scope 指定的路径范围
     */
    public FileResponse(Path file, Path scope)
    {
        this.file = file;
        this.scope = scope;
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
    public void writeToResponse(ActionContext context) throws IOException {
        HttpServerRequest req = context.getRequest();
        HttpServerResponse resp = req.response();
        Date fileLastModified = null;

        if (file != null) {
            if (scope != null) {
                String s = scope.relativize(file).toString();

                if (s.contains("..")) {
                    resp.setStatusCode(404).end();
                    return;
                }
            }

            if (!Files.isRegularFile(file)) {
                resp.setStatusCode(404).end();
                return;
            }

            stream = Files.newInputStream(file);
            contentType = Files.probeContentType(file);

            if (contentType == null) {
                contentType = URLConnection.guessContentTypeFromName(file.getFileName().toString());

                if (contentType == null) {
                    contentType = FileUtils.guessContentType(file.getFileName().toString());
                }
            }

            fileLastModified = new Date(Files.getLastModifiedTime(file).toMillis());

            resp.putHeader("Last-Modified",
                    StringUtils.dateToHttpDateString(fileLastModified));
            resp.putHeader("Accept-Range", "bytes");
        }

        if (stream != null) {
            if (contentType == null) {
                contentType = "text/plain";
            }

            resp.putHeader("Content-Type", contentType);

            try {
                long length = stream.available();
                long start = 0;
                boolean shouldSendPartial = true;
                List<RangeHeaderInfo> ranges = Collections.emptyList();

                String rangeHeader = req.getHeader("Range");

                if (!StringUtils.isEmpty(rangeHeader)) {
                    ranges = RequestUtils.parseRangeHeader(rangeHeader);

                    if (fileLastModified != null) {
                        String ifRangeHeader = req.getHeader("If-Range");

                        if (!StringUtils.isEmpty(ifRangeHeader)) {
                            Date ifRange = StringUtils.parseHttpDateString(ifRangeHeader);

                            if (fileLastModified.after(ifRange)) {
                                shouldSendPartial = false;
                            }
                        }
                    }
                } else {
                    shouldSendPartial = false;
                }

                if (shouldSendPartial) {
                    if (ranges.size() <= 0) {
                        resp.setStatusCode(416).end();
                        return;
                    }

                    // TODO: Support multiple ranges!
                    if (ranges.size() > 1) {
                        resp.setStatusCode(416).end();
                        return;
                    }

                    RangeHeaderInfo range = ranges.get(0);

                    if (range.getStart() < 0) {
                        resp.setStatusCode(416).end();
                        return;
                    }

                    if (range.getEnd() < 0) {
                        range.setEnd(length - 1);
                    } else if (range.getEnd() > length) {
                        resp.setStatusCode(416).end();
                        return;
                    }

                    resp.setStatusCode(206);
                    resp.putHeader("Content-Range", "bytes " + range.getStart() + "-" +
                            range.getEnd() + "/" + length);

                    start = range.getStart();
                    length = range.getEnd() - range.getStart() + 1;
                }

                resp.putHeader("Content-Length", String.valueOf(length));
                FileUtils.pumpInputStreamToWriteStream(stream, start, length, resp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                stream.close();
            }
        } else {
            throw new IOException("Invalid file response!");
        }
    }
}