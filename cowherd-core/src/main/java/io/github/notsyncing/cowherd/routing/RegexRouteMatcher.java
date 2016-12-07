package io.github.notsyncing.cowherd.routing;

import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.models.RouteInfo;
import io.github.notsyncing.cowherd.models.SimpleURI;
import io.github.notsyncing.cowherd.utils.RegexUtils;
import io.github.notsyncing.cowherd.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class RegexRouteMatcher extends RouteMatcher
{
    public RegexRouteMatcher(SimpleURI uri)
    {
        super(uri);
    }

    private MatchedRoute matchRoute(SimpleURI uri, RouteInfo info, boolean matchOnly)
    {
        if (("/".equals(uri.getPath())) && (info.isEntry())) {
            MatchedRoute mr = new MatchedRoute();
            mr.setRoute(info);
            return mr;
        }

        if ((info.getDomainPattern() != null) && (!info.getDomainPattern().matcher(uri.getHost()).find())) {
            return null;
        }

        if (info.getPathPattern() == null) {
            return null;
        }

        if (info.getPathPattern().matcher(StringUtils.stripSameCharAtStringHeader(uri.getPath(), '/')).find()) {
            if (matchOnly) {
                return new MatchedRoute();
            }

            MatchedRoute route = new MatchedRoute(extractRouteParameters(uri, info));
            route.setRoute(info);

            return route;
        }

        return null;
    }

    private List<Pair<String, String>> extractRouteParameters(SimpleURI uri, RouteInfo route)
    {
        List<Pair<String, String>> params = new ArrayList<>();

        if (route.getDomainPattern() != null) {
            RegexUtils.addMatchedGroupsToPairList(uri.getHost(), route.getDomainPattern(), params);
        }

        if (route.getPathPattern() != null) {
            RegexUtils.addMatchedGroupsToPairList(StringUtils.stripSameCharAtStringHeader(uri.getPath(), '/'),
                    route.getPathPattern(), params);
        }

        return params;
    }

    @Override
    protected MatchedRoute match(RouteInfo route, boolean matchOnly)
    {
        return matchRoute(uri, route, matchOnly);
    }
}
