package io.github.notsyncing.cowherd.server;

import io.github.notsyncing.cowherd.annotations.ContentType;
import io.github.notsyncing.cowherd.authentication.ActionAuthenticator;
import io.github.notsyncing.cowherd.authentication.annotations.ServiceActionAuthenticator;
import io.github.notsyncing.cowherd.exceptions.AuthenticationFailedException;
import io.github.notsyncing.cowherd.exceptions.FilterBreakException;
import io.github.notsyncing.cowherd.exceptions.ValidationFailedException;
import io.github.notsyncing.cowherd.models.*;
import io.github.notsyncing.cowherd.service.ComponentInstantiateType;
import io.github.notsyncing.cowherd.service.CowherdService;
import io.github.notsyncing.cowherd.service.DependencyInjector;
import io.github.notsyncing.cowherd.service.ServiceManager;
import io.github.notsyncing.cowherd.utils.FutureUtils;
import io.github.notsyncing.cowherd.utils.RequestUtils;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RequestExecutor
{
    @SuppressWarnings("unchecked")
    public static CompletableFuture<ActionResult> executeRequestedAction(Method requestedMethod, HttpServerRequest request,
                                                                         List<Pair<String, String>> parameters,
                                                                         List<HttpCookie> cookies,
                                                                         List<UploadFileInfo> uploads,
                                                                         Object... otherParams)
    {
        try {
            if (requestedMethod.isAnnotationPresent(ContentType.class)) {
                String contentType = requestedMethod.getAnnotation(ContentType.class).value();

                if (!StringUtils.isEmpty(contentType)) {
                    request.response().putHeader("Content-Type", contentType);
                }
            }

            Object[] targetParams;

            try {
                targetParams = RequestUtils.convertParameterListToMethodParameters(requestedMethod, request,
                        parameters, cookies, uploads, otherParams);
            } catch (ValidationFailedException e) {
                return FutureUtils.failed(e);
            }

            CowherdService service = ServiceManager.getServiceInstance((Class<? extends CowherdService>)requestedMethod.getDeclaringClass());
            Object result = requestedMethod.invoke(service, targetParams);

            if (result instanceof CompletableFuture) {
                CompletableFuture f = (CompletableFuture)result;
                return f.thenApply(r -> new ActionResult(requestedMethod, r));
            } else {
                ActionResult r = new ActionResult(requestedMethod, result);
                return CompletableFuture.completedFuture(r);
            }
        } catch (Exception e) {
            CompletableFuture f = new CompletableFuture();
            f.completeExceptionally(e);
            return f;
        }
    }

    public static CompletableFuture<ActionResult> executeRequestedWebSocketAction(Method requestedMethod, HttpServerRequest request,
                                                                                  List<Pair<String, String>> parameters,
                                                                                  List<HttpCookie> cookies,
                                                                                  Object... otherParams)
    {
        ServerWebSocket ws = request.upgrade();

        try {
            Object[] targetParams;

            try {
                targetParams = RequestUtils.convertParameterListToMethodParameters(requestedMethod, request,
                        parameters, cookies, null, ws, otherParams);
            } catch (ValidationFailedException e) {
                return FutureUtils.failed(e);
            }

            CowherdService service = ServiceManager.getServiceInstance((Class<? extends CowherdService>)requestedMethod.getDeclaringClass());
            Object result = requestedMethod.invoke(service, targetParams);

            if (result instanceof CompletableFuture) {
                return ((CompletableFuture)result).thenApply(r -> new WebSocketActionResult(requestedMethod, null));
            } else {
                return CompletableFuture.completedFuture(new WebSocketActionResult(requestedMethod, null));
            }
        } catch (Exception e) {
            return FutureUtils.failed(e);
        }
    }

    private static void prepareFilters(List<FilterExecutionInfo> matchedFilters)
    {
        if (matchedFilters != null) {
            for (FilterExecutionInfo filterInfo : matchedFilters) {
                if (filterInfo.getFilter().getInstantiateType() == ComponentInstantiateType.AlwaysNew) {
                    try {
                        filterInfo.getFilter().setFilterInstance(filterInfo.getFilter().getFilterClass().newInstance());
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }

                FilterContext context = new FilterContext();
                context.setFilterParameters(filterInfo.getParameters());

                filterInfo.setContext(context);
            }
        }
    }

    private static <T> CompletableFuture<T> executeFilters(List<FilterExecutionInfo> matchedFilters,
                                                             BiFunction<ServiceActionFilter, FilterContext, CompletableFuture<T>> filterFunc)
    {
        CompletableFuture<T> filterChain = null;

        if (matchedFilters != null) {
            for (FilterExecutionInfo filterInfo : matchedFilters) {
                ServiceActionFilter filter = filterInfo.getFilter().getFilterInstance();

                if (filterChain == null) {
                    filterChain = filterFunc.apply(filter, filterInfo.getContext());
                } else {
                    final ServiceActionFilter finalFilter = filter;
                    filterChain = filterChain.thenCompose(b -> {
                        if (b instanceof Boolean) {
                            if (!((Boolean)b)) {
                                return FutureUtils.failed(new FilterBreakException());
                            } else {
                                return filterFunc.apply(finalFilter, filterInfo.getContext());
                            }
                        } else {
                            return filterFunc.apply(finalFilter, filterInfo.getContext());
                        }
                    });
                }
            }
        }

        if (filterChain == null) {
            filterChain = CompletableFuture.completedFuture(null);
        }

        return filterChain;
    }

    private static CompletableFuture<Boolean> executeAuthenticators(Method m, FilterContext context)
    {
        if (m.getAnnotations() == null) {
            return CompletableFuture.completedFuture(true);
        }

        List<Annotation> authAnnos = Stream.of(m.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(ServiceActionAuthenticator.class))
                .collect(Collectors.toList());

        if (authAnnos.size() <= 0) {
            return CompletableFuture.completedFuture(true);
        }

        CompletableFuture<Boolean> f = null;

        for (Annotation a : authAnnos) {
            Class<? extends ActionAuthenticator> c = a.annotationType().getAnnotation(ServiceActionAuthenticator.class).value();
            ActionAuthenticator authenticator;

            try {
                authenticator = DependencyInjector.getComponent(c);
            } catch (Exception e) {
                e.printStackTrace();
                return FutureUtils.failed(e);
            }

            if (f == null) {
                f = authenticator.authenticate(a, context);
            } else {
                f = f.thenCompose(b -> {
                    if (!b) {
                        return FutureUtils.failed(new AuthenticationFailedException());
                    } else {
                        return authenticator.authenticate(a, context);
                    }
                });
            }
        }

        return f.thenCompose(b -> {
            if (!b) {
                return FutureUtils.failed(new AuthenticationFailedException());
            } else {
                return CompletableFuture.completedFuture(b);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static CompletableFuture<ActionResult> handleRequestedAction(Method requestedAction,
                                                                        List<FilterExecutionInfo> matchedFilters,
                                                                        List<Pair<String, String>> additionalParams,
                                                                        HttpServerRequest req,
                                                                        Object... otherParams)
    {
        if (!RequestUtils.checkIfHttpMethodIsAllowedOnAction(requestedAction, req.method())) {
            req.response()
                    .setStatusCode(403)
                    .setStatusMessage("Method " + req.method() + " is not allowed on " + requestedAction.getName())
                    .end();

            return CompletableFuture.completedFuture(new ActionResult());
        }

        prepareFilters(matchedFilters);

        CompletableFuture<Boolean> filterChain = executeFilters(matchedFilters, ServiceActionFilter::early);

        return filterChain.thenCompose(b -> {
            CompletableFuture<List<UploadFileInfo>> uploadFuture = RequestUtils.extractUploads(req);
            CompletableFuture<List<Pair<String, String>>> paramFuture = RequestUtils.extractRequestParameters(req,
                    additionalParams);

            final List<UploadFileInfo>[] uploadsRef = new List[1];
            final List<Pair<String, String>>[] paramsRef = new List[1];
            List<HttpCookie> cookies = RequestUtils.parseHttpCookies(req);
            final ActionResult[] result = new ActionResult[1];

            return paramFuture.thenCompose(p -> {
                paramsRef[0] = p;
                return uploadFuture;
            }).thenCompose(u -> {
                uploadsRef[0] = u;

                FilterContext context = new FilterContext();
                context.setRequestCookies(cookies);
                context.setRequest(req);
                context.setRequestUploads(uploadsRef[0]);
                context.setRequestParameters(paramsRef[0]);

                return executeAuthenticators(requestedAction, context);
            }).thenCompose(ab -> executeFilters(matchedFilters, (f, c) -> {
                c.setRequest(req);
                c.setRequestParameters(paramsRef[0]);
                c.setRequestUploads(uploadsRef[0]);
                c.setRequestCookies(cookies);
                return f.before(c);
            })).thenCompose(c -> executeRequestedAction(requestedAction, req, paramsRef[0], cookies, uploadsRef[0],
                    otherParams)
            ).thenCompose(r -> {
                result[0] = r;

                return executeFilters(matchedFilters, (f, c) -> {
                    c.setResult(r);
                    return f.after(c);
                });
            })
                    .thenApply(r -> r == null ? result[0] : r);
        });
    }

    public static CompletableFuture<ActionResult> handleRequestedWebSocketAction(Method requestedAction,
                                                                                 List<FilterExecutionInfo> matchedFilters,
                                                                                 List<Pair<String, String>> additionalParams,
                                                                                 HttpServerRequest req,
                                                                                 Object... otherParams)
    {
        prepareFilters(matchedFilters);

        CompletableFuture<Boolean> filterChain = executeFilters(matchedFilters, ServiceActionFilter::early);

        return filterChain.thenCompose(b -> {
            CompletableFuture<List<Pair<String, String>>> paramFuture = RequestUtils.extractRequestParameters(req,
                    additionalParams);

            final List<Pair<String, String>>[] paramsRef = new List[1];
            List<HttpCookie> cookies = RequestUtils.parseHttpCookies(req);

            return paramFuture.thenCompose(p -> {
                paramsRef[0] = p;

                FilterContext context = new FilterContext();
                context.setRequestCookies(cookies);
                context.setRequest(req);
                context.setRequestParameters(paramsRef[0]);

                return executeAuthenticators(requestedAction, context);
            }).thenCompose(ab -> executeFilters(matchedFilters, (f, c) -> {
                c.setRequest(req);
                c.setRequestParameters(paramsRef[0]);
                c.setRequestCookies(cookies);
                return f.before(c);
            })).thenCompose(c -> executeRequestedWebSocketAction(requestedAction, req, paramsRef[0], cookies,
                    otherParams));
        });
    }
}
