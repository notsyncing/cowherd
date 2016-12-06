package io.github.notsyncing.cowherd.routing;

import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.models.RouteInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MatchedRoute
{
    private RouteInfo route;
    private Method actionMethod;
    private List<Pair<String, String>> routeParameters;

    public MatchedRoute()
    {
        this(new ArrayList<>());
    }

    public MatchedRoute(List<Pair<String, String>> routeParameters)
    {
        this.routeParameters = routeParameters;
    }

    public RouteInfo getRoute()
    {
        return route;
    }

    public void setRoute(RouteInfo route)
    {
        this.route = route;
    }

    public Method getActionMethod()
    {
        return actionMethod;
    }

    public void setActionMethod(Method actionMethod)
    {
        this.actionMethod = actionMethod;
    }

    public List<Pair<String, String>> getRouteParameters()
    {
        return routeParameters;
    }

    public void setRouteParameters(List<Pair<String, String>> routeParameters)
    {
        this.routeParameters = routeParameters;
    }
}
