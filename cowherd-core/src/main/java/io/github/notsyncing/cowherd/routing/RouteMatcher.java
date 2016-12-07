package io.github.notsyncing.cowherd.routing;

import io.github.notsyncing.cowherd.models.RouteInfo;
import io.github.notsyncing.cowherd.models.SimpleURI;

public abstract class RouteMatcher
{
    protected SimpleURI uri;

    public RouteMatcher(SimpleURI uri) {
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