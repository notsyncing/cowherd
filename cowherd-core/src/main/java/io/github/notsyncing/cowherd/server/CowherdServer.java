package io.github.notsyncing.cowherd.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.github.notsyncing.cowherd.Cowherd;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.exceptions.AuthenticationFailedException;
import io.github.notsyncing.cowherd.exceptions.FilterBreakException;
import io.github.notsyncing.cowherd.exceptions.ValidationFailedException;
import io.github.notsyncing.cowherd.files.FileStorage;
import io.github.notsyncing.cowherd.models.ActionContext;
import io.github.notsyncing.cowherd.models.ActionResult;
import io.github.notsyncing.cowherd.models.CSRFToken;
import io.github.notsyncing.cowherd.models.WebSocketActionResult;
import io.github.notsyncing.cowherd.responses.ActionResponse;
import io.github.notsyncing.cowherd.routing.RouteManager;
import io.github.notsyncing.cowherd.service.ServiceManager;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.ConcurrentHashSet;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CowherdServer
{
    private Vertx vertx;
    private List<HttpServer> servers = new ArrayList<>();
    private FileStorage fileStorage;
    private CowherdLogger log = CowherdLogger.getInstance(this);
    private CowherdLogger accessLogger = CowherdLogger.getAccessLogger();
    private ConcurrentHashSet<CSRFToken> csrfTokens = new ConcurrentHashSet<>();

    private ScheduledExecutorService csrfTokenCleaner = Executors.newScheduledThreadPool(1, r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    public CowherdServer(Vertx vertx)
    {
        this.vertx = vertx;

        csrfTokenCleaner.scheduleAtFixedRate(() -> {
            Date now = new Date();
            csrfTokens.removeIf(t -> t.getExpireTime().before(now));
        }, 0, 1, TimeUnit.HOURS);
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

    private CompletableFuture<Void> processRequest(HttpServerRequest req)
    {
        String remoteAddr = req.remoteAddress().host();

        if (req.headers().contains("X-Forwarded-For")) {
            String s = req.getHeader("X-Forwarded-For");

            if (s != null) {
                int i = s.indexOf(",");

                if (i > 0) {
                    remoteAddr = s.substring(0, i);
                } else {
                    remoteAddr = s;
                }
            }
        } else if (req.headers().contains("X-Real-Ip")) {
            remoteAddr = req.getHeader("X-Real-Ip");
        }

        String accessLog = remoteAddr + ":" + req.remoteAddress().port() + " -> " +
                req.localAddress().host() + ":" + req.localAddress().port() + " (" + req.getHeader("User-Agent") +
                ") " + req.version() + " " + req.method() + " " + (req.isSSL() ? "SECURE " : "") + req.uri();

        long reqTimeStart = System.currentTimeMillis();

        ActionContext context = new ActionContext();
        context.setServer(this);
        context.setRequest(req);

        String finalRemoteAddr = remoteAddr + ":" + req.remoteAddress().port();

        req.response().exceptionHandler(ex -> {
            log.w("An exception occurred when writing response to " + finalRemoteAddr + ", uri " + req.uri(), ex);
        });

        return RouteManager.handleRequest(context).thenAccept(o -> {
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

            if (!req.response().ended()) {
                writeObjectToResponse(context, o);
            }

            long reqTimeEnd = System.currentTimeMillis();
            logAccess(req, accessLog, reqTimeEnd - reqTimeStart);
        }).exceptionally(ex -> {
            long reqTimeEnd = System.currentTimeMillis();

            if ((ex.getCause() instanceof AuthenticationFailedException) || (ex.getCause() instanceof FilterBreakException)) {
                req.response().setStatusCode(403);
                req.response().end();

                logAccess(req, accessLog, reqTimeEnd - reqTimeStart);
                return null;
            } else if (ex.getCause() instanceof ValidationFailedException) {
                req.response().setStatusCode(400);
                req.response().end();

                logAccess(req, accessLog, reqTimeEnd - reqTimeStart);
                return null;
            }

            Throwable e = (Throwable)ex;
            log.e("An exception was thrown when processing an action: ", e);

            String data = e.getMessage() + "\n" + StringUtils.exceptionStackToString(e);

            req.response().setStatusCode(500);
            req.response().setStatusMessage(e.getMessage());

            writeResponse(req.response(), data);
            logAccess(req, accessLog, reqTimeEnd - reqTimeStart);
            return null;
        });
    }

    private void logAccess(HttpServerRequest req, String accessLog, long accessProcessTime)
    {
        if (!CowherdConfiguration.isMakeAccessLoggerQuiet()) {
            accessLogger.i(accessLog + " " + req.response().getStatusCode() + " " + req.response().bytesWritten()
                    + (accessProcessTime > 0 ? " " + accessProcessTime + "ms" : ""));
        }
    }

    private void logAccess(HttpServerRequest req, String accessLog) {
        logAccess(req, accessLog, 0);
    }

    private void writeObjectToResponse(ActionContext context, ActionResult o)
    {
        HttpServerRequest req = context.getRequest();

        String ret;

        if (o.getResult() instanceof ActionResponse) {
            try {
                ((ActionResponse)o.getResult()).writeToResponse(context);
            } catch (Exception e) {
                log.e("An exception was thrown when writing response to client: ", e);
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

            if (context.getConfig().isEnumReturnsString()) {
                ret = o.getResult().toString();
            } else {
                ret = String.valueOf(((Enum) o.getResult()).ordinal());
            }

            writeResponse(req.response(), ret);
        } else {
            if (context.getConfig().isEnumReturnsString()) {
                ret = JSON.toJSONString(o.getResult(), SerializerFeature.WriteEnumUsingName);
            } else {
                ret = JSON.toJSONString(o.getResult());
            }

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
    }

    private void initServer()
    {
        if (vertx == null) {
            vertx = Vertx.vertx();

            Cowherd.dependencyInjector.registerComponent(Vertx.class, vertx);
        }

        fileStorage = new FileStorage(vertx);
        Cowherd.dependencyInjector.registerComponent(fileStorage);

        try {
            ServiceManager.instantiateSingletonServices();
        } catch (Exception e) {
            log.e("Failed to instantiate services!", e);
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

    /**
     * 异步停止服务器
     * @return 指示是否停止的 CompletableFuture 对象
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture stop()
    {
        log.i("Stopping server...");

        csrfTokens.clear();

        CompletableFuture f = new CompletableFuture();
        final int[] count = {0};

        for (HttpServer server : servers) {
            server.close(r -> {
                count[0]++;

                log.i("HTTP Server " + count[0] + " stopped.");

                if (count[0] >= servers.size()) {
                    f.complete(null);
                }
            });
        }

        return f.thenCompose(r -> {
            CompletableFuture f2 = new CompletableFuture();

            vertx.close(h -> {
                if (h.succeeded()) {
                    log.i("Server fully stopped.");

                    vertx = null;
                    f2.complete(null);
                } else {
                    f2.completeExceptionally(h.cause());
                }
            });

            return f2;
        });
    }

    public void addCSRFToken(String token) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        csrfTokens.add(new CSRFToken(token, calendar.getTime()));
    }

    public boolean checkAndRemoveCSRFToken(String token) {
        return csrfTokens.remove(new CSRFToken(token));
    }

    public boolean checkCSRFToken(String token) {
        return csrfTokens.contains(new CSRFToken(token));
    }
}
