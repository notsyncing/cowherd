package io.github.notsyncing.cowherd.server;

import io.github.notsyncing.cowherd.annotations.ContentType;
import io.github.notsyncing.cowherd.exceptions.FilterBreakException;
import io.github.notsyncing.cowherd.models.ActionResult;
import io.github.notsyncing.cowherd.models.FilterInfo;
import io.github.notsyncing.cowherd.models.UploadFileInfo;
import io.github.notsyncing.cowherd.service.CowherdService;
import io.github.notsyncing.cowherd.service.ComponentInstantiateType;
import io.github.notsyncing.cowherd.service.ServiceManager;
import io.github.notsyncing.cowherd.utils.RequestUtils;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.http.HttpServerRequest;

import java.lang.reflect.Method;
import java.net.HttpCookie;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RequestExecutor
{
    @SuppressWarnings("unchecked")
    public static CompletableFuture<ActionResult> executeRequestedAction(Method requestedMethod, HttpServerRequest request,
                                                                         Map<String, List<String>> parameters,
                                                                         List<UploadFileInfo> uploads)
    {
        try {
            if (requestedMethod.isAnnotationPresent(ContentType.class)) {
                String contentType = requestedMethod.getAnnotation(ContentType.class).value();

                if (!StringUtils.isEmpty(contentType)) {
                    request.response().putHeader("Content-Type", contentType);
                }
            }

            String cookieHeader = request.getHeader("Cookie");
            List<HttpCookie> cookies = null;

            if (cookieHeader != null) {
                cookies = HttpCookie.parse(cookieHeader);
            }

            Object[] targetParams = RequestUtils.convertParameterListToMethodParameters(requestedMethod, request,
                    parameters, cookies, uploads);
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

    private static CompletableFuture<Boolean> executeFilters(List<FilterInfo> matchedFilters)
    {
        CompletableFuture<Boolean> filterChain = null;

        if (matchedFilters != null) {
            for (FilterInfo filterInfo : matchedFilters) {
                ServiceActionFilter filter = filterInfo.getFilterInstance();

                if (filterInfo.getInstantiateType() == ComponentInstantiateType.AlwaysNew) {
                    try {
                        filter = filterInfo.getFilterClass().newInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }

                if (filterChain == null) {
                    filterChain = filter.filter();
                } else {
                    final ServiceActionFilter finalFilter = filter;
                    filterChain = filterChain.thenCompose(b -> {
                        if (!b) {
                            CompletableFuture<Boolean> cf = new CompletableFuture<>();
                            cf.completeExceptionally(new FilterBreakException());
                            return cf;
                        } else {
                            return finalFilter.filter();
                        }
                    });
                }
            }
        }

        if (filterChain == null) {
            filterChain = CompletableFuture.completedFuture(true);
        }

        return filterChain;
    }

    @SuppressWarnings("unchecked")
    public static CompletableFuture<ActionResult> handleRequestedAction(Method requestedAction,
                                                                        List<FilterInfo> matchedFilters,
                                                                        Map<String, List<String>> additionalParams,
                                                                        HttpServerRequest req)
    {
        if (!RequestUtils.checkIfHttpMethodIsAllowedOnAction(requestedAction, req.method())) {
            req.response()
                    .setStatusCode(403)
                    .setStatusMessage("Method " + req.method() + " is not allowed on " + requestedAction.getName())
                    .end();

            return CompletableFuture.completedFuture(new ActionResult());
        }

        CompletableFuture<Boolean> filterChain = executeFilters(matchedFilters);

        return filterChain.thenCompose(b -> {
            CompletableFuture<List<UploadFileInfo>> uploadFuture = RequestUtils.extractUploads(req);
            CompletableFuture<Map<String, List<String>>> paramFuture = RequestUtils.extractRequestParameters(req,
                    additionalParams);

            final List<UploadFileInfo>[] uploadsRef = new List[1];
            final Map<String, List<String>>[] paramsRef = new Map[1];

            return paramFuture.thenCompose(p -> {
                paramsRef[0] = p;
                return uploadFuture;
            }).thenCompose(u -> {
                uploadsRef[0] = u;
                return executeRequestedAction(requestedAction, req, paramsRef[0], uploadsRef[0]);
            });
        });
    }
}
