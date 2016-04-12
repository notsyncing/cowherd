package io.github.notsyncing.cowherd.models;

import io.github.notsyncing.cowherd.server.CowherdServer;
import io.vertx.core.http.HttpServerRequest;

import java.lang.reflect.Method;

public class ActionContext
{
    private CowherdServer server;
    private HttpServerRequest request;
    private Method actionMethod;

    public ActionContext()
    {
    }

    public ActionContext(HttpServerRequest request)
    {
        this.request = request;
    }

    public CowherdServer getServer()
    {
        return server;
    }

    public void setServer(CowherdServer server)
    {
        this.server = server;
    }

    public HttpServerRequest getRequest()
    {
        return request;
    }

    public void setRequest(HttpServerRequest request)
    {
        this.request = request;
    }

    public Method getActionMethod()
    {
        return actionMethod;
    }

    public void setActionMethod(Method actionMethod)
    {
        this.actionMethod = actionMethod;
    }
}
