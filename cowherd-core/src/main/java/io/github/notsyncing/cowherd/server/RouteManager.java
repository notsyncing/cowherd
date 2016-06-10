package io.github.notsyncing.cowherd.server;

import io.github.notsyncing.cowherd.annotations.*;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.commons.RouteType;
import io.github.notsyncing.cowherd.exceptions.InvalidServiceActionException;
import io.github.notsyncing.cowherd.models.*;
import io.github.notsyncing.cowherd.responses.ActionResponse;
import io.github.notsyncing.cowherd.responses.FileResponse;
import io.github.notsyncing.cowherd.responses.ViewResponse;
import io.github.notsyncing.cowherd.service.CowherdService;
import io.github.notsyncing.cowherd.utils.FutureUtils;
import io.github.notsyncing.cowherd.utils.RouteUtils;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RouteManager
{
    private static Map<RouteInfo, Method> routes = new ConcurrentSkipListMap<>();
    //private static Map<RouteInfo, Method> routes = new ConcurrentHashMap<>();

    private static CowherdLogger log = CowherdLogger.getInstance(RouteManager.class);

    public static Map<RouteInfo, Method> getRoutes()
    {
        return routes;
    }

    public static void addRoute(RouteInfo route, Method target)
    {
        if (routes.containsKey(route)) {
            log.w("Route " + route + " already mapped to action " + routes.get(route) +
                    ", will be overwritten to " + target);
        }

        routes.put(route, target);

        log.d("Add route " + route + " to action " + target);
    }

    public static void addRoutesInService(Class<? extends CowherdService> service, CowherdServiceInfo serviceInfo) throws InvalidServiceActionException
    {
        Route serviceRoute = service.getAnnotation(Route.class);
        RouteInfo serviceRouteInfo = serviceInfo.getCustomRoute();

        if (serviceRouteInfo == null) {
            serviceRouteInfo = new RouteInfo();

            if (serviceRoute != null) {
                serviceRouteInfo.setDomain(serviceRoute.domain());
                serviceRouteInfo.setPath(serviceRoute.value());
            } else {
                serviceRouteInfo.setPath(service.getSimpleName() + "/");
            }
        }

        for (Method m : service.getMethods()) {
            if (!m.isAnnotationPresent(Exported.class)) {
                continue;
            }

            RouteInfo info = new RouteInfo();

            if (!m.isAnnotationPresent(Route.class)) {
                info.setPath(StringUtils.appendUrl(serviceRouteInfo.getPath(), m.getName()));
                info.setDomain(serviceRouteInfo.getDomain());
            } else {
                Route route = m.getAnnotation(Route.class);
                info.setPath(route.value());
                info.setDomain(route.domain());
                info.setEntry(route.entry());
            }

            if (m.isAnnotationPresent(WebSocket.class)) {
                info.setType(RouteType.WebSocket);
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

    private static List<FilterExecutionInfo> findMatchedFilters(URI uri, Method m)
    {
        List<FilterExecutionInfo> filters = new ArrayList<>();

        if ((m.isAnnotationPresent(Filter.class)) || (m.isAnnotationPresent(Filters.class))) {
            Filter[] list = m.getAnnotationsByType(Filter.class);

            for (Filter f : list) {
                FilterInfo filter = FilterManager.getNormalFilters().get(f.value());

                if (filter == null) {
                    continue;
                }

                FilterExecutionInfo info = new FilterExecutionInfo(filter);

                if (f.parameters().length > 0) {
                    for (FilterParameter p : f.parameters()) {
                        info.addParameter(p.name(), p.value());
                    }
                }

                filters.add(info);
            }
        }

        filters.addAll(FilterManager.getGlobalFilters().stream()
                .map(FilterExecutionInfo::new)
                .collect(Collectors.toList()));

        List<FilterExecutionInfo> routedFilters = FilterManager.getRoutedFilters().entrySet().stream()
                .filter(e -> RouteUtils.matchRoute(uri, e.getKey()))
                .map(Map.Entry::getValue)
                .map(FilterExecutionInfo::new)
                .collect(Collectors.toList());

        filters.addAll(routedFilters);

        return filters;
    }

    public static CompletableFuture<ActionResult> handleRequest(HttpServerRequest request)
    {
        log.d("Request: " + request.path());

        String contentType = request.getHeader("Content-Type");

        if ((contentType != null) && (contentType.toLowerCase().contains("multipart/form-data"))) {
            request.setExpectMultipart(true);
        }

        URI uri = RouteUtils.resolveUriFromRequest(request);
        Map.Entry<RouteInfo, Method> p = findMatchedAction(uri);

        if (p == null) {
            ActionResponse resp = null;

            if (request.method() == HttpMethod.GET) {
                try {
                    resp = handleFileRequest(request);
                } catch (Exception e) {
                    e.printStackTrace();

                    CompletableFuture<ActionResult> f = new CompletableFuture<>();
                    f.completeExceptionally(e);
                    return f;
                }
            }

            if ((resp == null) && (!request.response().ended())) {
                log.d(" ... no route");
                request.response().setStatusCode(404).end();
                return CompletableFuture.completedFuture(new ActionResult());
            } else {
                return CompletableFuture.completedFuture(new ActionResult(null, resp));
            }
        }

        RouteInfo r = p.getKey();
        Method m = p.getValue();
        log.d(" ... action " + m);

        if (!m.isAnnotationPresent(DisableCORS.class)) {
            if (request.headers().contains("Origin")) {
                String origin = request.getHeader("Origin");
                String remoteAddr = request.remoteAddress().host();
                boolean allow = true;

                if ((!remoteAddr.equals("127.0.0.1")) && (!remoteAddr.equals("localhost")) && (!remoteAddr.equals("0:0:0:0:0:0:0:1"))) {
                    if (!Stream.of(CowherdConfiguration.getAllowOrigins()).anyMatch(origin::equals)) {
                        origin = "NOT_ALLOWED";
                        allow = false;
                    }
                }

                if (allow) {
                    if (request.headers().contains("Access-Control-Request-Headers")) {
                        request.response().putHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"));
                    }

                    if (request.headers().contains("Access-Control-Request-Method")) {
                        request.response().putHeader("Access-Control-Allow-Methods", request.getHeader("Access-Control-Request-Method"));
                    }
                }

                request.response().putHeader("Access-Control-Allow-Origin", origin);

                if (request.method() == HttpMethod.OPTIONS) {
                    request.response().end();
                    return CompletableFuture.completedFuture(new ActionResult());
                }
            }
        }

        if (r.getType() == RouteType.Http) {
            return RequestExecutor.handleRequestedAction(m, findMatchedFilters(uri, m),
                    RouteUtils.extractRouteParameters(uri, r), request);
        } else if (r.getType() == RouteType.WebSocket) {
            return RequestExecutor.handleRequestedWebSocketAction(m, findMatchedFilters(uri, m),
                    RouteUtils.extractRouteParameters(uri, r), request);
        }

        return FutureUtils.failed(new UnsupportedOperationException("Unknown route type " + r.getType() + " in route " + r));
    }

    private static ActionResponse handleFileRequest(HttpServerRequest request) throws IOException, ParseException, URISyntaxException
    {
        boolean needSend = true;

        String reqPath = request.path();

        if ("/".equals(reqPath)) {
            reqPath = "index.html";
        } else if (reqPath.startsWith("/")) {
            reqPath = reqPath.substring(1);
        }

        for (Path contextRoot : CowherdConfiguration.getContextRoots()) {
            if (contextRoot.getName(contextRoot.getNameCount() - 1).toString().equals("$")) {
                contextRoot = Paths.get(CowherdConfiguration.class.getResource("/APP_ROOT").toURI());
            }

            Path file = contextRoot.resolve(reqPath);

            if (!file.toAbsolutePath().toString().startsWith(contextRoot.toString())) {
                continue;
            }

            if (Files.isRegularFile(file)) {
                String fn = contextRoot.relativize(file).toString();

                if ((fn.endsWith(".html")) && (CowherdConfiguration.isEveryHtmlIsTemplate())) {
                    fn = fn.substring(0, fn.length() - 5);
                    log.d(" ... view: " + file);
                    return new ViewResponse(null, fn);
                }

                String ifModifiedSince = request.getHeader("If-Modified-Since");

                if (!StringUtils.isEmpty(ifModifiedSince)) {
                    long fileModifyTime = Files.getLastModifiedTime(file).toMillis() / 1000;
                    long reqQueryTime = StringUtils.parseHttpDateString(ifModifiedSince).getTime() / 1000;

                    if (fileModifyTime <= reqQueryTime) {
                        request.response().putHeader("Last-Modified", StringUtils.dateToHttpDateString(new Date(fileModifyTime)));
                        request.response().setStatusCode(304).end();
                        needSend = false;
                    }
                }

                if (needSend) {
                    log.d(" ... local file: " + file);
                    return new FileResponse(file);
                } else {
                    log.d(" ... local file: " + file + " (not modified)");
                    return null;
                }
            }
        }

        return null;
    }
}
