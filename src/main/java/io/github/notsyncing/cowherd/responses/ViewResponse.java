package io.github.notsyncing.cowherd.responses;

import io.github.notsyncing.cowherd.annotations.Route;
import io.github.notsyncing.cowherd.exceptions.InvalidViewResponseException;
import io.github.notsyncing.cowherd.models.ActionContext;
import io.vertx.core.http.HttpServerResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class ViewResponse implements ActionResponse
{
    private Object model;

    public ViewResponse(Object model)
    {
        this.model = model;
    }

    @Override
    public CompletableFuture writeToResponse(ActionContext context) throws IOException
    {
        CompletableFuture future = new CompletableFuture();
        TemplateEngine eng = context.getServer().getTemplateEngine();
        Method action = context.getActionMethod();
        HttpServerResponse resp = context.getRequest().response();

        if (!action.isAnnotationPresent(Route.class)) {
            future.completeExceptionally(new InvalidViewResponseException("Action " + action.toString() +
                    " has no route annotation with view page!"));
            return future;
        }

        Route route = action.getAnnotation(Route.class);
        String templateName = route.value();

        if (templateName.endsWith(".html")) {
            templateName = templateName.substring(0, templateName.length() - 5);
        }

        Context c = new Context();
        c.setVariable("model", model);

        String s = eng.process(templateName, c);
        resp.putHeader("Content-Type", "text/html;charset=UTF-8");
        resp.putHeader("Content-Length", String.valueOf(s.getBytes("utf-8").length));
        resp.write(s);
        resp.end();

        return future;
    }
}
