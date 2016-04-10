package io.github.notsyncing.cowherd.server;

import com.alibaba.fastjson.JSON;
import io.github.notsyncing.cowherd.commons.GlobalStorage;
import io.github.notsyncing.cowherd.responses.ActionResponse;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CompletableFuture;

public class CowherdServer
{
    Vertx vertx;
    HttpServer server;

    void writeResponse(HttpServerResponse resp, byte[] data)
    {
        resp.putHeader("Content-Length", String.valueOf(data.length));
        resp.write(Buffer.buffer(data));
        resp.end();
    }

    void writeResponse(HttpServerResponse resp, String data)
    {
        try {
            writeResponse(resp, data.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            writeResponse(resp, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    void processRequest(HttpServerRequest req)
    {
        RouteManager.handleRequest(req).thenAccept(o -> {
            if (o == null) {
                if (!req.response().ended()) {
                    req.response().end();
                }

                return;
            }

            String ret;

            if (o instanceof ActionResponse) {
                try {
                    ((ActionResponse)o).writeToResponse(req.response());
                } catch (IOException e) {
                    e.printStackTrace();
                    req.response().setStatusCode(500);
                    req.response().setStatusMessage(e.getMessage());
                }

                if (!req.response().ended()) {
                    req.response().end();
                }
            } else if (o instanceof String) {
                ret = (String)o;
                writeResponse(req.response(), ret);
            } else {
                ret = JSON.toJSONString(o);

                if (!req.response().headers().contains("Content-Type")) {
                    req.response().putHeader("Content-Type", "application/json");
                }

                writeResponse(req.response(), ret);
            }
        }).exceptionally(ex -> {
            Throwable e = (Throwable)ex;
            e.printStackTrace();

            String data = e.getMessage() + "\n" + StringUtils.exceptionStackToString(e);

            req.response().setStatusCode(500);
            req.response().setStatusMessage(e.getMessage());

            writeResponse(req.response(), data);
            return null;
        });
    }

    public void start()
    {
        vertx = Vertx.vertx();
        server = vertx.createHttpServer();
        server.requestHandler(this::processRequest);
        server.listen(GlobalStorage.getListenPort());

        System.out.println("CowherdServer: listening at port " + GlobalStorage.getListenPort());
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture stop()
    {
        CompletableFuture f = new CompletableFuture();

        server.close(f::complete);

        return f;
    }
}
