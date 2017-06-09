package io.github.notsyncing.cowherd;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;
import io.github.notsyncing.cowherd.annotations.NoAutoRegister;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.models.RouteInfo;
import io.github.notsyncing.cowherd.routing.RouteManager;
import io.github.notsyncing.cowherd.server.CowherdLogger;
import io.github.notsyncing.cowherd.server.CowherdServer;
import io.github.notsyncing.cowherd.server.FilterManager;
import io.github.notsyncing.cowherd.server.ServiceActionFilter;
import io.github.notsyncing.cowherd.service.*;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Cowherd
{
    private Vertx vertx = Vertx.vertx();
    private CowherdServer server;
    private CowherdLogger log = CowherdLogger.getInstance(this);
    private List<Class<? extends CowherdPart>> parts = new ArrayList<>();
    private List<CowherdPart> partInstances = new ArrayList<>();

    public static DependencyInjector dependencyInjector;

    public static void main(String[] args)
    {
        Cowherd app = new Cowherd();
        app.start();
    }

    private void initParts(ScanResult classScanResult)
    {
        classScanResult.getNamesOfClassesImplementing(CowherdPart.class)
                .forEach(c -> {
                    try {
                        parts.add((Class)Class.forName(c));
                    } catch (ClassNotFoundException e) {
                        log.w("Failed to load part " + c + ": class not found: " + e.getMessage());
                    }
                });

        parts.forEach(c -> {
            try {
                CowherdPart p = c.newInstance();
                p.init();

                partInstances.add(p);

                log.i("Loaded part: " + c.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void start(FastClasspathScanner classpathScanner)
    {
        if (classpathScanner == null) {
            classpathScanner = createClasspathScanner();
        }

        CowherdDependencyInjector.setScanner(classpathScanner);

        if (dependencyInjector == null) {
            dependencyInjector = new CowherdDependencyInjector(false);
        }

        configure();

        addInternalServices();

        ScanResult classScanResult = classpathScanner.scan();

        initParts(classScanResult);

        dependencyInjector.init();

        scanClasses(classScanResult);

        startServer();
    }

    public void start()
    {
        start(null);
    }

    private void configure()
    {
        CowherdLogger.loggerConfigChanged();

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

        dependencyInjector.registerComponent(Vertx.class, vertx);
    }

    private void addInternalServices()
    {
        if (CowherdConfiguration.isEnableInjectedService()) {
            RouteInfo apiRoute = new RouteInfo();
            apiRoute.setPath(CowherdConfiguration.getApiServiceRoute());
            apiRoute.setDomain(CowherdConfiguration.getApiServiceDomain());

            ServiceManager.addServiceClass(CowherdAPIService.class, apiRoute);
        }
    }

    public FastClasspathScanner createClasspathScanner()
    {
        return new FastClasspathScanner("-io.vertx", "-org.junit", "-io.netty", "-javax", "-javassist",
                "-jar:gragent.jar", "-jar:jfxrt.jar", "-jar:jfxswt.jar", "-jar:idea_rt.jar", "-jar:junit-rt.jar",
                "-scala", "-com.github.mauricio", "-APP_ROOT");
    }

    private void scanClasses(ScanResult s)
    {
        s.getNamesOfSubclassesOf(CowherdService.class)
                .forEach(c -> {
                    try {
                        Class clazz = Class.forName(c);

                        if (clazz.isAnnotationPresent(NoAutoRegister.class)) {
                            return;
                        }

                        ServiceManager.addServiceClass(clazz);
                    } catch (ClassNotFoundException e) {
                        log.w("Failed to load service class " + c + ": class not found: " + e.getMessage());
                    }
                });

        s.getNamesOfClassesImplementing(ServiceActionFilter.class)
                .forEach(c -> {
                    try {
                        FilterManager.addFilterClass((Class)Class.forName(c));
                    } catch (ClassNotFoundException e) {
                        log.w("Failed to load service action filter class " + c + ": class not found: " + e.getMessage());
                    }
                });
    }

    private void startServer()
    {
        server = new CowherdServer(vertx);

        dependencyInjector.registerComponent(server);

        server.start();
    }

    public CompletableFuture stop()
    {
        partInstances.forEach(CowherdPart::destroy);

        ServiceManager.clear();
        RouteManager.reset();

        return server.stop();
    }
}
