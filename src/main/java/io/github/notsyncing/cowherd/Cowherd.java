package io.github.notsyncing.cowherd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.notsyncing.cowherd.annotations.Component;
import io.github.notsyncing.cowherd.commons.GlobalStorage;
import io.github.notsyncing.cowherd.models.RouteInfo;
import io.github.notsyncing.cowherd.server.CowherdServer;
import io.github.notsyncing.cowherd.server.FilterManager;
import io.github.notsyncing.cowherd.server.ServiceActionFilter;
import io.github.notsyncing.cowherd.service.CowherdAPIService;
import io.github.notsyncing.cowherd.service.CowherdService;
import io.github.notsyncing.cowherd.service.DependencyInjector;
import io.github.notsyncing.cowherd.service.ServiceManager;
import io.github.notsyncing.cowherd.utils.StringUtils;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class Cowherd
{
    @Parameter(names = { "-p", "--port" })
    int listenPort = 8080;

    @Parameter(names = { "-r", "--context-root" })
    String contextRoot;

    private CowherdServer server;

    public static void main(String[] args)
    {
        Cowherd app = new Cowherd();
        new JCommander(app, args);

        app.start();
    }

    public void start()
    {
        configure();

        addInternalServices();

        scanClasses();

        startServer();
    }

    private void configure()
    {
        GlobalStorage.setListenPort(listenPort);

        if (!StringUtils.isEmpty(contextRoot)) {
            GlobalStorage.setContextRoot(Paths.get(contextRoot));
        }
    }

    private void addInternalServices()
    {
        RouteInfo apiRoute = new RouteInfo();
        apiRoute.setPath(GlobalStorage.getApiServiceRoute());
        apiRoute.setDomain(GlobalStorage.getApiServiceDomain());

        ServiceManager.addServiceClass(CowherdAPIService.class, apiRoute);
    }

    private void scanClasses()
    {
        FastClasspathScanner s = new FastClasspathScanner()
                .matchClassesWithAnnotation(Component.class, DependencyInjector::registerComponent)
                .scan();

        DependencyInjector.classScanCompleted();

        s.matchSubclassesOf(CowherdService.class, ServiceManager::addServiceClass)
                .matchClassesImplementing(ServiceActionFilter.class, c -> FilterManager.addFilterClass(c))
                .scan();
    }

    private void startServer()
    {
        server = new CowherdServer();
        server.start();
    }

    public CompletableFuture stop()
    {
        return server.stop();
    }
}
