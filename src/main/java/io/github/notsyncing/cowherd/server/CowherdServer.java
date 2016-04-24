package io.github.notsyncing.cowherd.server;

import com.alibaba.fastjson.JSON;
import io.github.notsyncing.cowherd.commons.GlobalStorage;
import io.github.notsyncing.cowherd.models.ActionContext;
import io.github.notsyncing.cowherd.models.ActionResult;
import io.github.notsyncing.cowherd.responses.ActionResponse;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CompletableFuture;

public class CowherdServer
{
    private Vertx vertx;
    private HttpServer server;
    private TemplateEngine templateEngine;

    public TemplateEngine getTemplateEngine()
    {
        return templateEngine;
    }

    private void writeResponse(HttpServerResponse resp, byte[] data)
    {
        resp.putHeader("Content-Length", String.valueOf(data.length));
        resp.write(Buffer.buffer(data));
        resp.end();
    }

    private void writeResponse(HttpServerResponse resp, String data)
    {
        try {
            writeResponse(resp, data.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            writeResponse(resp, e.getMessage());
        }
    }

    private void processRequest(HttpServerRequest req)
    {
        RouteManager.handleRequest(req).thenAccept(o -> {
            if (o.getResult() == null) {
                if (!req.response().ended()) {
                    req.response().end();
                }

                return;
            }

            writeObjectToResponse(req, o);
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

    private void writeObjectToResponse(HttpServerRequest req, ActionResult o)
    {
        String ret;

        if (o.getResult() instanceof ActionResponse) {
            ActionContext context = new ActionContext();
            context.setActionMethod(o.getActionMethod());
            context.setServer(this);
            context.setRequest(req);

            try {
                ((ActionResponse)o.getResult()).writeToResponse(context);
            } catch (IOException e) {
                e.printStackTrace();
                req.response().setStatusCode(500);
                req.response().setStatusMessage(e.getMessage());
            }

            if (!req.response().ended()) {
                req.response().end();
            }
        } else if (o.getResult() instanceof String) {
            ret = (String) o.getResult();
            writeResponse(req.response(), ret);
        } else if (o.getResult() instanceof Enum) {
            if (!req.response().headers().contains("Content-Type")) {
                req.response().putHeader("Content-Type", "text/plain");
            }

            ret = String.valueOf(((Enum)o.getResult()).ordinal());
            writeResponse(req.response(), ret);
        } else {
            ret = JSON.toJSONString(o.getResult());

            if (!req.response().headers().contains("Content-Type")) {
                req.response().putHeader("Content-Type", "application/json");
            }

            writeResponse(req.response(), ret);
        }
    }

    public void start()
    {
        initServer();

        initTemplateEngine();
    }

    private void initServer()
    {
        vertx = Vertx.vertx();
        server = vertx.createHttpServer();
        server.requestHandler(this::processRequest);
        server.listen(GlobalStorage.getListenPort());

        System.out.println("CowherdServer: listening at port " + GlobalStorage.getListenPort());
    }

    private void initTemplateEngine()
    {
        templateEngine = new TemplateEngine();

        ClassLoaderTemplateResolver clr = new ClassLoaderTemplateResolver();
        clr.setPrefix("APP_ROOT");
        clr.setSuffix(".html");
        templateEngine.addTemplateResolver(clr);

        FileTemplateResolver fr = new FileTemplateResolver();
        fr.setPrefix(GlobalStorage.getContextRoot().toAbsolutePath().toString());
        fr.setSuffix(".html");
        templateEngine.addTemplateResolver(fr);
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture stop()
    {
        CompletableFuture f = new CompletableFuture();

        server.close(f::complete);

        return f;
    }
}
