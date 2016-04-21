package io.github.notsyncing.cowherd.service;

import io.github.notsyncing.cowherd.models.ActionResult;
import io.github.notsyncing.cowherd.models.UploadFileInfo;
import io.github.notsyncing.cowherd.server.RequestExecutor;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.lang.reflect.Method;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class CowherdService
{
    protected CompletableFuture delegateTo(String serviceName, String actionName, HttpServerRequest request,
                                                         Map<String, List<String>> parameters, List<UploadFileInfo> uploads)
    {
        Method m = ServiceManager.getServiceAction(serviceName, actionName);

        if (m == null) {
            request.response()
                    .setStatusCode(404)
                    .setStatusMessage("Action " + serviceName + "." + actionName + " is not found!")
                    .end();

            return CompletableFuture.completedFuture(null);
        }

        return RequestExecutor.executeRequestedAction(m, request, parameters, uploads).thenApply(ActionResult::getResult);
    }

    protected void putCookie(HttpServerResponse response, HttpCookie cookie)
    {
        response.putHeader("Set-Cookie", cookie.toString());
    }

    protected void putCookies(HttpServerResponse response, HttpCookie... cookies)
    {
        if (cookies == null) {
            return;
        }

        for (HttpCookie c : cookies) {
            putCookie(response, c);
        }
    }
}
