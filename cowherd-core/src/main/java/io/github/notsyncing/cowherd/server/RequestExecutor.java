package io.github.notsyncing.cowherd.server;

import io.github.notsyncing.cowherd.Cowherd;
import io.github.notsyncing.cowherd.annotations.ContentType;
import io.github.notsyncing.cowherd.authentication.ActionAuthenticator;
import io.github.notsyncing.cowherd.authentication.annotations.ServiceActionAuthenticator;
import io.github.notsyncing.cowherd.exceptions.AuthenticationFailedException;
import io.github.notsyncing.cowherd.exceptions.FilterBreakException;
import io.github.notsyncing.cowherd.exceptions.ValidationFailedException;
import io.github.notsyncing.cowherd.models.*;
import io.github.notsyncing.cowherd.service.ComponentInstantiateType;
import io.github.notsyncing.cowherd.service.CowherdService;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RequestExecutor
{
    private static CowherdLogger log = CowherdLogger.getInstance(RequestExecutor.class);

    @SuppressWarnings("unchecked")
    public static CompletableFuture<ActionResult> executeRequestedAction(ActionContext context,
                                                                         List<Pair<String, String>> parameters,
                                                                         List<HttpCookie> cookies,
                                                                         List<UploadFileInfo> uploads,
                                                                         Object... otherParams)
    {
        Method requestedMethod = context.getActionMethod();
        HttpServerRequest request = context.getRequest();

        try {
            if (requestedMethod.isAnnotationPresent(ContentType.class)) {
                String contentType = requestedMethod.getAnnotation(ContentType.class).value();

                if (!StringUtils.isEmpty(contentType)) {
                    request.response().putHeader("Content-Type", contentType);
                }
            }

            Object[] targetParams;

            try {
                targetParams = RequestUtils.convertParameterListToMethodParameters(context, parameters, cookies,
                        uploads, otherParams);
            } catch (ValidationFailedException e) {
                return FutureUtils.failed(e);
            }

            CowherdService service = ServiceManager.getServiceInstance((Class<? extends CowherdService>)requestedMethod.getDeclaringClass());
            Object result = requestedMethod.invoke(service, targetParams);

            if (result instanceof CompletableFuture) {
                CompletableFuture f = (CompletableFuture)result;
                return f.thenApply(r -> new ActionResult(context, r));
            } else {
                ActionResult r = new ActionResult(context, result);
                return CompletableFuture.completedFuture(r);
            }
        } catch (Exception e) {
            CompletableFuture f = new CompletableFuture();
            f.completeExceptionally(e);
            return f;
        }
    }

    public static CompletableFuture<ActionResult> executeRequestedWebSocketAction(ActionContext context,
                                                                                  List<Pair<String, String>> parameters,
                                                                                  List<HttpCookie> cookies,
                                                                                  Object... otherParams)
    {
        Method requestedMethod = context.getActionMethod();
        HttpServerRequest request = context.getRequest();

        ServerWebSocket ws = request.upgrade();

        try {
            Object[] targetParams;

            try {
                targetParams = RequestUtils.convertParameterListToMethodParameters(context, parameters, cookies,
                        null, ws, otherParams);
            } catch (ValidationFailedException e) {
                return FutureUtils.failed(e);
            }

            CowherdService service = ServiceManager.getServiceInstance((Class<? extends CowherdService>)requestedMethod.getDeclaringClass());
            Object result = requestedMethod.invoke(service, targetParams);

            if (result instanceof CompletableFuture) {
                return ((CompletableFuture)result).thenApply(r -> new WebSocketActionResult(context, null));
            } else {
                return CompletableFuture.completedFuture(new WebSocketActionResult(context, null));
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
                        log.e("Failed to create an instance of filter " + filterInfo.getFilter().getFilterClass(), e);
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
                authenticator = Cowherd.dependencyInjector.getComponent(c);
            } catch (Exception e) {
                log.e("Failed to get an instance of authenticator " + c, e);
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
    public static CompletableFuture<ActionResult> handleRequestedAction(ActionContext context,
                                                                        List<FilterExecutionInfo> matchedFilters,
                                                                        List<Pair<String, String>> additionalParams,
                                                                        RequestContext req,
                                                                        Object... otherParams)
    {
        Method requestedAction = context.getActionMethod();

        if (!RequestUtils.checkIfHttpMethodIsAllowedOnAction(requestedAction, req.getMethod())) {
            req.getResponse()
                    .setStatusCode(403)
                    .setStatusMessage("Method " + req.getMethod() + " is not allowed on " + requestedAction.getName())
                    .end();

            return CompletableFuture.completedFuture(new ActionResult());
        }

        prepareFilters(matchedFilters);

        CompletableFuture<Boolean> filterChain = executeFilters(matchedFilters, ServiceActionFilter::early);

        return filterChain.thenCompose(b -> {
            req.getParameters().addAll(additionalParams);

            List<HttpCookie> cookies = RequestUtils.parseHttpCookies(req.getRequest());
            final ActionResult[] result = new ActionResult[1];

            FilterContext filterContext = new FilterContext();
            filterContext.setRequestCookies(cookies);
            filterContext.setRequest(req.getRequest());
            filterContext.setRequestUploads(req.getUploads());
            filterContext.setRequestParameters(req.getParameters());

            return executeAuthenticators(requestedAction, filterContext)
                    .thenCompose(ab -> executeFilters(matchedFilters, (f, c) -> {
                        c.setRequest(req.getRequest());
                        c.setRequestParameters(req.getParameters());
                        c.setRequestUploads(req.getUploads());
                        c.setRequestCookies(cookies);
                        return f.before(c);
                    }))
                    .thenCompose(c -> executeRequestedAction(context, req.getParameters(), cookies, req.getUploads(),
                            otherParams))
                    .thenCompose(r -> {
                        result[0] = r;

                        return executeFilters(matchedFilters, (f, c) -> {
                            c.setResult(r);
                            return f.after(c);
                        });
                    }).thenApply(r -> r == null ? result[0] : r);
        });
    }

    public static CompletableFuture<ActionResult> handleRequestedWebSocketAction(ActionContext context,
                                                                                 List<FilterExecutionInfo> matchedFilters,
                                                                                 List<Pair<String, String>> additionalParams,
                                                                                 RequestContext req,
                                                                                 Object... otherParams)
    {
        Method requestedAction = context.getActionMethod();

        prepareFilters(matchedFilters);

        CompletableFuture<Boolean> filterChain = executeFilters(matchedFilters, ServiceActionFilter::early);

        return filterChain.thenCompose(b -> {
            req.getParameters().addAll(additionalParams);

            List<HttpCookie> cookies = RequestUtils.parseHttpCookies(req.getRequest());

            FilterContext filterContext = new FilterContext();
            filterContext.setRequestCookies(cookies);
            filterContext.setRequest(req.getRequest());
            filterContext.setRequestParameters(req.getParameters());

            return executeAuthenticators(requestedAction, filterContext)
                    .thenCompose(ab -> executeFilters(matchedFilters, (f, c) -> {
                        c.setRequest(req.getRequest());
                        c.setRequestParameters(req.getParameters());
                        c.setRequestCookies(cookies);
                        return f.before(c);
                    }))
                    .thenCompose(c -> executeRequestedWebSocketAction(context, req.getParameters(), cookies,
                            otherParams));
        });
    }
}
