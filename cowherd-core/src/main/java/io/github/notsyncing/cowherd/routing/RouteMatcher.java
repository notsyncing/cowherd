package io.github.notsyncing.cowherd.routing;

import io.github.notsyncing.cowherd.models.RouteInfo;

import java.net.URI;

public abstract class RouteMatcher
{
    protected URI uri;

    public RouteMatcher(URI uri) {
        this.uri = uri;
    }

    protected abstract MatchedRoute match(RouteInfo route, boolean matchOnly);

    public MatchedRoute match(RouteInfo route)
    {
        return match(route, false);
    }

    public boolean matchOnly(RouteInfo route)
    {
        return match(route, true) != null;
    }
}