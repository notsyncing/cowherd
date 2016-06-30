package io.github.notsyncing.cowherd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.notsyncing.cowherd.annotations.Component;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.models.RouteInfo;
import io.github.notsyncing.cowherd.server.CowherdLogger;
import io.github.notsyncing.cowherd.server.CowherdServer;
import io.github.notsyncing.cowherd.server.FilterManager;
import io.github.notsyncing.cowherd.server.ServiceActionFilter;
import io.github.notsyncing.cowherd.service.CowherdAPIService;
import io.github.notsyncing.cowherd.service.CowherdService;
import io.github.notsyncing.cowherd.service.DependencyInjector;
import io.github.notsyncing.cowherd.service.ServiceManager;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Cowherd
{
    @Parameter(names = { "-p", "--port" })
    int listenPort = 8080;

    @Parameter(names = { "-r", "--context-root" })
    List<String> contextRoots = new ArrayList<>();

    private Vertx vertx = Vertx.vertx();
    private CowherdServer server;
    private CowherdLogger log = CowherdLogger.getInstance(this);

    public static void main(String[] args)
    {
        Cowherd app = new Cowherd();
        new JCommander(app, args);

        app.start();
    }

    public void start(FastClasspathScanner classpathScanner)
    {
        configure();

        addInternalServices();

        scanClasses(classpathScanner);

        startServer();
    }

    public void start()
    {
        start(null);
    }

    private void configure()
    {
        CowherdLogger.loggerConfigChanged();

        CowherdConfiguration.setListenPort(listenPort);

        if (contextRoots.size() > 0) {
            CowherdConfiguration.setContextRoots(contextRoots.stream().map(s -> Paths.get(s)).toArray(Path[]::new));
        }

        InputStream s = getClass().getResourceAsStream("/cowherd.config");

        if (s != null) {
            String data;

            try {
                data = StringUtils.streamToString(s);
            } catch (Exception e) {
                log.e("Failed to load configuration file: " + e.getMessage(), e);
                return;
            }

            JsonObject config = new JsonObject(data);
            CowherdConfiguration.fromConfig(config);

            log.i("Loaded configuration file.");

            for (Path p : CowherdConfiguration.getContextRoots()) {
                String cr;

                if (p.getName(p.getNameCount() - 1).toString().equals("$")) {
                    URL url = CowherdConfiguration.class.getResource("/APP_ROOT");

                    if (url == null) {
                        log.w("No APP_ROOT found in classpath!");
                        continue;
                    }

                    cr = url.toString();
                } else {
                    cr = p.toAbsolutePath().toString();
                }

                log.i("Context root: " + cr);
            }

            if (CowherdConfiguration.isEveryHtmlIsTemplate()) {
                log.w("You've enabled everyHtmlIsTemplate, please keep in mind that this is not suitable for production!");
            }
        } else {
            log.i("No configuration file found.");
        }

        log.i("Cowherd web server is starting...");

        DependencyInjector.registerComponent(Vertx.class, vertx);
    }

    private void addInternalServices()
    {
        RouteInfo apiRoute = new RouteInfo();
        apiRoute.setPath(CowherdConfiguration.getApiServiceRoute());
        apiRoute.setDomain(CowherdConfiguration.getApiServiceDomain());

        ServiceManager.addServiceClass(CowherdAPIService.class, apiRoute);
    }

    public FastClasspathScanner createClasspathScanner()
    {
        return new FastClasspathScanner("-io.vertx", "-org.junit", "-io.netty", "-javax", "-javassist",
                "-jar:gragent.jar", "-jar:jfxrt.jar", "-jar:jfxswt.jar", "-jar:idea_rt.jar", "-jar:junit-rt.jar",
                "-APP_ROOT");
    }

    private void scanClasses(FastClasspathScanner s)
    {
        if (s == null) {
            s = createClasspathScanner();
        }

        s.matchClassesWithAnnotation(Component.class, DependencyInjector::registerComponent)
                .scan();

        DependencyInjector.classScanCompleted();

        s.matchSubclassesOf(CowherdService.class, ServiceManager::addServiceClass)
                .matchClassesImplementing(ServiceActionFilter.class, c -> FilterManager.addFilterClass(c))
                .scan();
    }

    private void startServer()
    {
        server = new CowherdServer(vertx);

        DependencyInjector.registerComponent(server);

        server.start();
    }

    public CompletableFuture stop()
    {
        return server.stop();
    }
}
