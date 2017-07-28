package io.github.notsyncing.cowherd.thymeleaf;

import io.github.notsyncing.cowherd.annotations.Route;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.exceptions.InvalidViewResponseException;
import io.github.notsyncing.cowherd.models.ActionContext;
import io.github.notsyncing.cowherd.responses.ActionResponse;
import io.vertx.core.http.HttpServerResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务方法返回的视图响应，用于向客户端呈现一个视图，同时向视图传递一个模型对象
 */
public class ViewResponse<T> implements ActionResponse
{
    private T model;
    private Map<String, Object> addModels = new HashMap<>();
    private String viewName = null;

    public ViewResponse()
    {
    }

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

    public void setModel(T model)
    {
        this.model = model;
    }

    public ViewResponse<T> addModel(String name, Object model)
    {
        addModels.put(name, model);
        return this;
    }

    @Override
    public void writeToResponse(ActionContext context) throws IOException
    {
        TemplateEngine eng = CowherdThymeleafPart.templateEngine;
        Method action = context.getActionMethod().getMethod();
        HttpServerResponse resp = context.getRequest().response();

        if (!CowherdConfiguration.isEveryHtmlIsTemplate()) {
            if (!action.isAnnotationPresent(Route.class)) {
                throw new RuntimeException(new InvalidViewResponseException("Action " + action.toString() +
                        " has no route annotation with view page!"));
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

        addModels.forEach(c::setVariable);

        String s = eng.process(templateName, c);
        resp.putHeader("Content-Type", "text/html;charset=UTF-8");
        resp.putHeader("Content-Length", String.valueOf(s.getBytes("utf-8").length));
        resp.write(s);
        resp.end();
    }
}
