package io.github.notsyncing.cowherd.service;

import io.github.notsyncing.cowherd.models.ActionResult;
import io.github.notsyncing.cowherd.models.UploadFileInfo;
import io.github.notsyncing.cowherd.server.RequestExecutor;
import io.vertx.core.http.HttpServerRequest;

import java.lang.reflect.Method;
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
}
