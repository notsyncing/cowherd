package io.github.notsyncing.cowherd.server;

import com.alibaba.fastjson.JSON;
import io.github.notsyncing.cowherd.annotations.Exported;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.exceptions.AuthenticationFailedException;
import io.github.notsyncing.cowherd.exceptions.FilterBreakException;
import io.github.notsyncing.cowherd.exceptions.ValidationFailedException;
import io.github.notsyncing.cowherd.files.FileStorage;
import io.github.notsyncing.cowherd.models.ActionContext;
import io.github.notsyncing.cowherd.models.ActionResult;
import io.github.notsyncing.cowherd.models.WebSocketActionResult;
import io.github.notsyncing.cowherd.responses.ActionResponse;
import io.github.notsyncing.cowherd.service.DependencyInjector;
import io.github.notsyncing.cowherd.service.ServiceManager;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CowherdServer
{
    private Vertx vertx;
    private List<HttpServer> servers = new ArrayList<>();
    private TemplateEngine templateEngine;
    private FileStorage fileStorage;
    private CowherdLogger log = CowherdLogger.getInstance(this);
    private CowherdLogger accessLogger = CowherdLogger.getAccessLogger();

    public CowherdServer(Vertx vertx)
    {
        this.vertx = vertx;
    }

    public TemplateEngine getTemplateEngine()
    {
        return templateEngine;
    }

    public FileStorage getFileStorage()
    {
        return fileStorage;
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
        String accessLog = req.remoteAddress().host() + ":" + req.remoteAddress().port() + " -> " +
                req.localAddress().host() + ":" + req.localAddress().port() + " (" + req.getHeader("User-Agent") +
                ") " + req.version() + " " + req.method() + " " + (req.isSSL() ? "SECURE " : "") + req.uri();

        RouteManager.handleRequest(req).thenAccept(o -> {
            if (o instanceof WebSocketActionResult) {
                logAccess(req, accessLog);
                return;
            }

            if (o.getResult() == null) {
                if (!req.response().ended()) {
                    req.response().end();
                }

                logAccess(req, accessLog);
                return;
            }

            writeObjectToResponse(req, o);
            logAccess(req, accessLog);
        }).exceptionally(ex -> {
            if ((ex.getCause() instanceof AuthenticationFailedException) || (ex.getCause() instanceof FilterBreakException)) {
                req.response().setStatusCode(403);
                req.response().end();

                logAccess(req, accessLog);
                return null;
            } else if (ex.getCause() instanceof ValidationFailedException) {
                req.response().setStatusCode(400);
                req.response().end();

                logAccess(req, accessLog);
                return null;
            }

            Throwable e = (Throwable)ex;
            e.printStackTrace();

            String data = e.getMessage() + "\n" + StringUtils.exceptionStackToString(e);

            req.response().setStatusCode(500);
            req.response().setStatusMessage(e.getMessage());

            writeResponse(req.response(), data);
            logAccess(req, accessLog);
            return null;
        });
    }

    private void logAccess(HttpServerRequest req, String accessLog)
    {
        accessLogger.i(accessLog + " " + req.response().getStatusCode() + " " + req.response().bytesWritten());
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

    /**
     * 启动服务器
     */
    public void start()
    {
        initServer();

        initTemplateEngine();
    }

    private void initServer()
    {
        if (vertx == null) {
            vertx = Vertx.vertx();

            DependencyInjector.registerComponent(Vertx.class, vertx);
        }

        fileStorage = new FileStorage(vertx);
        DependencyInjector.registerComponent(fileStorage);

        try {
            ServiceManager.instantiateSingletonServices();
        } catch (Exception e) {
            e.printStackTrace();
            log.e("Failed to instantiate services!");
        }

        HttpServerOptions options = new HttpServerOptions()
                .setCompressionSupported(CowherdConfiguration.isEnableCompression());

        int count = CowherdConfiguration.getWorkers();

        if (count <= 0) {
            count = Runtime.getRuntime().availableProcessors() - 1;

            if (count <= 0) {
                count = 1;
            }
        }

        log.i("Starting " + count + " http servers...");

        for (int i = 0; i < count; i++) {
            HttpServer server = vertx.createHttpServer(options)
                    .requestHandler(this::processRequest)
                    .listen(CowherdConfiguration.getListenPort());

            servers.add(server);
        }

        log.i("Listening at port " + CowherdConfiguration.getListenPort());
    }

    private void initTemplateEngine()
    {
        templateEngine = new TemplateEngine();
        templateEngine.addDialect(new Java8TimeDialect());

        for (Path r : CowherdConfiguration.getContextRoots()) {
            if (r.getName(r.getNameCount() - 1).toString().equals("$")) {
                ClassLoaderTemplateResolver clr = new ClassLoaderTemplateResolver();
                clr.setCharacterEncoding("utf-8");
                clr.setPrefix("APP_ROOT/");
                clr.setSuffix(".html");
                clr.setTemplateMode(TemplateMode.HTML);
                clr.setCacheable(!CowherdConfiguration.isEveryHtmlIsTemplate());
                templateEngine.addTemplateResolver(clr);
                continue;
            }

            FileTemplateResolver fr = new FileTemplateResolver();
            fr.setCharacterEncoding("utf-8");
            fr.setPrefix(r.toString() + "/");
            fr.setSuffix(".html");
            fr.setTemplateMode(TemplateMode.HTML);
            fr.setCacheable(!CowherdConfiguration.isEveryHtmlIsTemplate());
            templateEngine.addTemplateResolver(fr);
        }
    }

    /**
     * 异步停止服务器
     * @return 指示是否停止的 CompletableFuture 对象
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture stop()
    {
        CompletableFuture f = new CompletableFuture();
        final int[] count = {0};

        for (HttpServer server : servers) {
            server.close(r -> {
                count[0]++;

                if (count[0] >= servers.size()) {
                    f.complete(null);
                }
            });
        }

        return f;
    }
}
