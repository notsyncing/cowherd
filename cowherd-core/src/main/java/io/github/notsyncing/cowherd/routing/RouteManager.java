package io.github.notsyncing.cowherd.routing;

import io.github.notsyncing.cowherd.annotations.*;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.commons.RouteType;
import io.github.notsyncing.cowherd.exceptions.InvalidServiceActionException;
import io.github.notsyncing.cowherd.models.*;
import io.github.notsyncing.cowherd.responses.ActionResponse;
import io.github.notsyncing.cowherd.responses.FileResponse;
import io.github.notsyncing.cowherd.server.CowherdLogger;
import io.github.notsyncing.cowherd.server.FilterManager;
import io.github.notsyncing.cowherd.server.RequestExecutor;
import io.github.notsyncing.cowherd.service.CowherdService;
import io.github.notsyncing.cowherd.utils.FutureUtils;
import io.github.notsyncing.cowherd.utils.RequestUtils;
import io.github.notsyncing.cowherd.utils.RouteUtils;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
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
    private static Path classpathContextRoot;

    public static Map<RouteInfo, Method> getRoutes()
    {
        return routes;
    }

    public static void reset()
    {
        routes.clear();
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

                if (route.subRoute()) {
                    info.setPath(serviceRouteInfo.getPath() + route.value());
                    info.setFastRoute(serviceRouteInfo.isFastRoute());
                } else {
                    info.setPath(route.value());
                }

                info.setDomain(route.domain());
                info.setEntry(route.entry());
                info.setViewPath(StringUtils.isEmpty(route.viewPath()) ? null : route.viewPath());
            }

            if (m.isAnnotationPresent(WebSocket.class)) {
                info.setType(RouteType.WebSocket);
            }

            m.setAccessible(true);
            addRoute(info, m);
        }
    }

    public static MatchedRoute findMatchedAction(SimpleURI uri)
    {
        RouteMatcher fastRouteMatcher = new FastRouteMatcher(uri);
        RouteMatcher regexRouteMatcher = new RegexRouteMatcher(uri);

        for (Map.Entry<RouteInfo, Method> r : routes.entrySet()) {
            RouteMatcher matcher;

            if (r.getKey().isFastRoute()) {
                matcher = fastRouteMatcher;
            } else {
                matcher = regexRouteMatcher;
            }

            MatchedRoute mr = matcher.match(r.getKey());

            if (mr != null) {
                mr.setActionMethod(r.getValue());
                return mr;
            }
        }

        return null;
    }

    private static List<FilterExecutionInfo> findMatchedFilters(SimpleURI uri, Method m)
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

        RouteMatcher fastRouteMatcher = new FastRouteMatcher(uri);
        RouteMatcher regexRouteMatcher = new RegexRouteMatcher(uri);

        List<FilterExecutionInfo> routedFilters = FilterManager.getRoutedFilters().entrySet().stream()
                .filter(e -> {
                    if (e.getKey().isFastRoute()) {
                        return fastRouteMatcher.matchOnly(e.getKey());
                    } else {
                        return regexRouteMatcher.matchOnly(e.getKey());
                    }
                })
                .map(Map.Entry::getValue)
                .map(FilterExecutionInfo::new)
                .collect(Collectors.toList());

        filters.addAll(routedFilters);

        return filters;
    }

    public static CompletableFuture<ActionResult> handleRequest(HttpServerRequest request)
    {
        log.d("Request: " + request.path());

        return RequestUtils.toRequestContext(request).thenCompose(req -> {
            SimpleURI uri = RouteUtils.resolveUriFromRequest(request);
            MatchedRoute p = findMatchedAction(uri);

            if (p == null) {
                ActionResponse resp = null;

                if (req.getMethod() == HttpMethod.GET) {
                    try {
                        resp = handleFileRequest(req);
                    } catch (Exception e) {
                        log.e("An exception was thrown when processing file request " + uri, e);

                        CompletableFuture<ActionResult> f = new CompletableFuture<>();
                        f.completeExceptionally(e);
                        return f;
                    }
                }

                if ((resp == null) && (!req.getResponse().ended())) {
                    log.d(" ... no route");
                    req.getResponse().setStatusCode(404).end();
                    return CompletableFuture.completedFuture(new ActionResult());
                } else {
                    return CompletableFuture.completedFuture(new ActionResult(null, resp));
                }
            }

            RouteInfo r = p.getRoute();
            Method m = p.getActionMethod();
            log.d(" ... action " + m);

            if (!m.isAnnotationPresent(DisableCORS.class)) {
                if (req.getHeaders().contains("Origin")) {
                    String origin = req.getHeaders().get("Origin");
                    String remoteAddr = request.remoteAddress().host();
                    boolean allow = true;

                    if ((!remoteAddr.equals("127.0.0.1")) && (!remoteAddr.equals("localhost"))
                            && (!remoteAddr.equals("0:0:0:0:0:0:0:1"))
                            && (!origin.equals("file://"))) {
                        if (!Stream.of(CowherdConfiguration.getAllowOrigins()).anyMatch(origin::equals)) {
                            origin = "NOT_ALLOWED";
                            allow = false;
                        }
                    }

                    if (allow) {
                        if (req.getHeaders().contains("Access-Control-Request-Headers")) {
                            req.getResponse().putHeader("Access-Control-Allow-Headers",
                                    req.getHeaders().get("Access-Control-Request-Headers"));
                        }

                        if (req.getHeaders().contains("Access-Control-Request-Method")) {
                            req.getResponse().putHeader("Access-Control-Allow-Methods",
                                    req.getHeaders().get("Access-Control-Request-Method"));
                        }

                        req.getResponse().putHeader("Access-Control-Allow-Credentials", "true");
                    }

                    req.getResponse().putHeader("Access-Control-Allow-Origin", origin);

                    if (req.getMethod() == HttpMethod.OPTIONS) {
                        req.getResponse().end();
                        return CompletableFuture.completedFuture(new ActionResult());
                    }

                    if (!allow) {
                        req.getResponse().setStatusCode(403).end();
                        return CompletableFuture.completedFuture(new ActionResult());
                    }
                }
            }

            if (r.getType() == RouteType.Http) {
                return RequestExecutor.handleRequestedAction(m, findMatchedFilters(uri, m),
                        p.getRouteParameters(), req, r.getOtherParameters());
            } else if (r.getType() == RouteType.WebSocket) {
                return RequestExecutor.handleRequestedWebSocketAction(m, findMatchedFilters(uri, m),
                        p.getRouteParameters(), req, r.getOtherParameters());
            }

            return FutureUtils.failed(new UnsupportedOperationException("Unknown route type " + r.getType() +
                    " in route " + r));
        });
    }

    private static ActionResponse handleFileRequest(RequestContext request) throws IOException, ParseException, URISyntaxException
    {
        boolean needSend = true;

        String reqPath = StringUtils.stripSameCharAtStringHeader(request.getPath(), '/');

        if ("/".equals(reqPath)) {
            reqPath = "index.html";
        } else if (reqPath.startsWith("/")) {
            reqPath = reqPath.substring(1);
        }

        for (Path contextRoot : CowherdConfiguration.getContextRoots()) {
            if (contextRoot.getName(contextRoot.getNameCount() - 1).toString().equals("$")) {
                if (classpathContextRoot == null) {
                    URI uri = CowherdConfiguration.class.getResource("/APP_ROOT").toURI();

                    if (uri.getScheme().equals("jar")) {
                        String[] parts = uri.toString().split("!");
                        classpathContextRoot = FileSystems.newFileSystem(URI.create(parts[0]), new HashMap<>())
                                .getPath(parts[1]);
                    } else {
                        classpathContextRoot = Paths.get(uri);
                    }
                }

                contextRoot = classpathContextRoot;
            }

            Path file = contextRoot.resolve(reqPath);

            if (!file.toAbsolutePath().toString().startsWith(contextRoot.toString())) {
                continue;
            }

            if (Files.isRegularFile(file)) {
                String ifModifiedSince = request.getHeaders().get("If-Modified-Since");

                if (!StringUtils.isEmpty(ifModifiedSince)) {
                    long fileModifyTime = Files.getLastModifiedTime(file).toMillis() / 1000;
                    long reqQueryTime = StringUtils.parseHttpDateString(ifModifiedSince).getTime() / 1000;

                    if (fileModifyTime <= reqQueryTime) {
                        request.getResponse().putHeader("Last-Modified", StringUtils.dateToHttpDateString(new Date(fileModifyTime)));
                        request.getResponse().setStatusCode(304).end();
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
