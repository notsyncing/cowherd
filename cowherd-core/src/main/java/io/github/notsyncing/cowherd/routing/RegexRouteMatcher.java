package io.github.notsyncing.cowherd.routing;

import io.github.notsyncing.cowherd.models.RouteInfo;
import io.github.notsyncing.cowherd.utils.RouteUtils;

import java.net.URI;

public class RegexRouteMatcher extends RouteMatcher
{
    public RegexRouteMatcher(URI uri)
    {
        super(uri);
    }

    @Override
    protected MatchedRoute match(RouteInfo route, boolean matchOnly)
    {
        return RouteUtils.matchRoute(uri, route, matchOnly);
    }
}
