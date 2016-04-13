package io.github.notsyncing.cowherd.server;

import io.github.notsyncing.cowherd.annotations.Exported;
import io.github.notsyncing.cowherd.annotations.Filter;
import io.github.notsyncing.cowherd.annotations.Route;
import io.github.notsyncing.cowherd.commons.GlobalStorage;
import io.github.notsyncing.cowherd.exceptions.InvalidServiceActionException;
import io.github.notsyncing.cowherd.models.*;
import io.github.notsyncing.cowherd.responses.FileResponse;
import io.github.notsyncing.cowherd.service.CowherdService;
import io.github.notsyncing.cowherd.utils.RouteUtils;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

public class RouteManager
{
    private static Map<RouteInfo, Method> routes = new ConcurrentSkipListMap<>((o1, o2) -> o2.getPath().length() - o1.getPath().length());

    public static Map<RouteInfo, Method> getRoutes()
    {
        return routes;
    }

    public static void addRoute(RouteInfo route, Method target)
    {
        routes.put(route, target);
    }

    public static void addRoutesInService(Class<? extends CowherdService> service, CowherdServiceInfo serviceInfo) throws InvalidServiceActionException
    {
        Route serviceRoute = service.getAnnotation(Route.class);
        RouteInfo serviceRouteInfo = serviceInfo.getCustomRoute();

        if ((serviceRouteInfo == null) && (serviceRoute != null)) {
            serviceRouteInfo = new RouteInfo();
            serviceRouteInfo.setDomain(serviceRoute.domain());
            serviceRouteInfo.setPath(serviceRoute.value());
        }

        for (Method m : service.getMethods()) {
            if (!m.isAnnotationPresent(Exported.class)) {
                continue;
            }

            RouteInfo info = new RouteInfo();

            if (!m.isAnnotationPresent(Route.class)) {
                if (serviceRouteInfo == null) {
                    continue;
                }

                info.setPath(StringUtils.appendUrl(serviceRouteInfo.getPath(), m.getName()));
                info.setDomain(serviceRouteInfo.getDomain());
            } else {
                Route route = m.getAnnotation(Route.class);
                info.setPath(route.value());
                info.setDomain(route.domain());
                info.setEntry(route.entry());
            }

            addRoute(info, m);
        }
    }

    private static Map.Entry<RouteInfo, Method> findMatchedAction(URI uri)
    {
        Optional<Map.Entry<RouteInfo, Method>> entry = routes.entrySet().stream()
                .filter(e -> RouteUtils.matchRoute(uri, e.getKey()))
                .findFirst();

        if (!entry.isPresent()) {
            return null;
        }

        return entry.get();
    }

    private static List<FilterInfo> findMatchedFilters(URI uri, Method m)
    {
        List<FilterInfo> filters = new ArrayList<>();

        if (m.isAnnotationPresent(Filter.class)) {
            Filter[] list = m.getAnnotationsByType(Filter.class);

            for (Filter f : list) {
                Optional<FilterInfo> filter = FilterManager.getNormalFilters().entrySet().stream()
                        .filter(e -> e.getKey().equals(f.value()))
                        .map(Map.Entry::getValue)
                        .findFirst();

                if (!filter.isPresent()) {
                    continue;
                }

                filters.add(filter.get());
            }
        }

        filters.addAll(FilterManager.getGlobalFilters());

        List<FilterInfo> routedFilters = FilterManager.getRoutedFilters().entrySet().stream()
                .filter(e -> RouteUtils.matchRoute(uri, e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        filters.addAll(routedFilters);

        return filters;
    }

    public static CompletableFuture<ActionResult> handleRequest(HttpServerRequest request)
    {
        String contentType = request.getHeader("Content-Type");

        if ((contentType != null) && (contentType.toLowerCase().contains("multipart/form-data"))) {
            request.setExpectMultipart(true);
        }

        URI uri = RouteUtils.resolveUriFromRequest(request);
        Map.Entry<RouteInfo, Method> p = findMatchedAction(uri);

        if (p == null) {
            boolean processed = false;

            if (request.method() == HttpMethod.GET) {
                try {
                    processed = handleFileRequest(request);
                } catch (Exception e) {
                    e.printStackTrace();

                    CompletableFuture<ActionResult> f = new CompletableFuture<>();
                    f.completeExceptionally(e);
                    return f;
                }
            }

            if (!processed) {
                request.response().setStatusCode(404).end();
            }

            return CompletableFuture.completedFuture(new ActionResult());
        }

        Method m = p.getValue();
        return RequestExecutor.handleRequestedAction(m, findMatchedFilters(uri, m),
                RouteUtils.extractRouteParameters(uri, p.getKey()), request);
    }

    private static boolean handleFileRequest(HttpServerRequest request) throws IOException, ParseException
    {
        boolean needSend = true;

        String reqPath = request.path();

        if ("/".equals(reqPath)) {
            reqPath = "index.html";
        } else if (reqPath.startsWith("/")) {
            reqPath = reqPath.substring(1);
        }

        Path file = GlobalStorage.getContextRoot().resolve(reqPath);

        if (!file.toAbsolutePath().toString().startsWith(GlobalStorage.getContextRoot().toString())) {
            request.response().setStatusCode(404).end();
            return false;
        }

        if (Files.isRegularFile(file)) {
            String ifModifiedSince = request.getHeader("If-Modified-Since");

            if (!StringUtils.isEmpty(ifModifiedSince)) {
                long fileModifyTime = Files.getLastModifiedTime(file).toMillis();
                long reqQueryTime = StringUtils.parseHttpDateString(ifModifiedSince).getTime();

                if (fileModifyTime <= reqQueryTime) {
                    request.response().putHeader("Last-Modified", StringUtils.dateToHttpDateString(new Date(fileModifyTime)));
                    request.response().setStatusCode(304).end();
                    needSend = false;
                }
            }

            if (needSend) {
                FileResponse fileResp = new FileResponse(file);
                fileResp.writeToResponse(new ActionContext(request));
                return true;
            }
        }

        return !needSend;
    }
}
