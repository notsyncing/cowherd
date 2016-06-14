package io.github.notsyncing.cowherd.responses;

import io.github.notsyncing.cowherd.annotations.Route;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.exceptions.InvalidViewResponseException;
import io.github.notsyncing.cowherd.models.ActionContext;
import io.vertx.core.http.HttpServerResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * 服务方法返回的视图响应，用于向客户端呈现一个视图，同时向视图传递一个模型对象
 */
public class ViewResponse<T> implements ActionResponse
{
    private T model;
    private String viewName = null;

    /**
     * 实例化视图响应对象
     * @param model 向视图传递的模型对象
     */
    public ViewResponse(T model)
    {
        this.model = model;
    }

    public ViewResponse(T model, String viewName)
    {
        this.model = model;
        this.viewName = viewName;
    }

    public T getModel()
    {
        return model;
    }

    @Override
    public CompletableFuture writeToResponse(ActionContext context) throws IOException
    {
        CompletableFuture future = new CompletableFuture();
        TemplateEngine eng = context.getServer().getTemplateEngine();
        Method action = context.getActionMethod();
        HttpServerResponse resp = context.getRequest().response();

        if (!CowherdConfiguration.isEveryHtmlIsTemplate()) {
            if (!action.isAnnotationPresent(Route.class)) {
                future.completeExceptionally(new InvalidViewResponseException("Action " + action.toString() +
                        " has no route annotation with view page!"));
                return future;
            }
        }

        String templateName = viewName;

        if (templateName == null) {
            Route route = action.getAnnotation(Route.class);
            templateName = route.viewPath();

            if (templateName.endsWith(".html")) {
                templateName = templateName.substring(0, templateName.length() - 5);
            }
        }

        Context c = new Context();
        c.setVariable("model", model);
        c.setVariable("request", context.getRequest());

        String s = eng.process(templateName, c);
        resp.putHeader("Content-Type", "text/html;charset=UTF-8");
        resp.putHeader("Content-Length", String.valueOf(s.getBytes("utf-8").length));
        resp.write(s);
        resp.end();

        return future;
    }
}
