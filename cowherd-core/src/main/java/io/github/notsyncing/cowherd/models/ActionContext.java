package io.github.notsyncing.cowherd.models;

import io.github.notsyncing.cowherd.server.CowherdServer;
import io.vertx.core.http.HttpServerRequest;

public class ActionContext
{
    private CowherdServer server;
    private HttpServerRequest request;
    private RouteInfo route;
    private ActionMethodInfo actionMethod;
    private ActionConfig config;

    public ActionContext()
    {
        config = new ActionConfig();
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

    public RouteInfo getRoute() {
        return route;
    }

    public void setRoute(RouteInfo route) {
        this.route = route;
    }

    public ActionMethodInfo getActionMethod()
    {
        return actionMethod;
    }

    public void setActionMethod(ActionMethodInfo actionMethod)
    {
        this.actionMethod = actionMethod;
    }

    public ActionConfig getConfig()
    {
        return config;
    }
}
