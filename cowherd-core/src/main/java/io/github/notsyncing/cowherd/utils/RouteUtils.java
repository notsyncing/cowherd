package io.github.notsyncing.cowherd.utils;

import io.github.notsyncing.cowherd.annotations.httpmethods.*;
import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.models.RouteInfo;
import io.vertx.core.http.HttpServerRequest;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteUtils
{
    public static boolean matchRoute(URI uri, RouteInfo info)
    {
        if (("/".equals(uri.getPath())) && (info.isEntry())) {
            return true;
        }

        if ((info.getDomainPattern() != null) && (!info.getDomainPattern().matcher(uri.getHost()).find())) {
            return false;
        }

        if (info.getPathPattern() == null) {
            return false;
        }

        return (info.getPathPattern().matcher(StringUtils.stripSameCharAtStringHeader(uri.getPath(), '/')).find());
    }

    public static List<Pair<String, String>> extractRouteParameters(URI uri, RouteInfo route)
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

    public static String getActionHttpMethodString(Method m)
    {
        if (m.isAnnotationPresent(HttpAnyMethod.class)) {
            return "get";
        } else if (m.isAnnotationPresent(HttpGet.class)) {
            return "get";
        } else if (m.isAnnotationPresent(HttpPost.class)) {
            return "post";
        } else if (m.isAnnotationPresent(HttpPut.class)) {
            return "put";
        } else if (m.isAnnotationPresent(HttpDelete.class)) {
            return "delete";
        } else if (m.isAnnotationPresent(HttpHead.class)) {
            return "head";
        } else if (m.isAnnotationPresent(HttpOptions.class)) {
            return "options";
        } else {
            return "get";
        }
    }

    public static URI resolveUriFromRequest(HttpServerRequest request)
    {
        String reqPath = StringUtils.stripSameCharAtStringHeader(request.path(), '/');
        int homeCharPos = reqPath.lastIndexOf("~");

        if (homeCharPos >= 0) {
            reqPath = reqPath.substring(homeCharPos + 1);
        }

        URI reqUri;
        URI uri = null;

        try {
            reqUri = new URI(request.absoluteURI());

            if (homeCharPos >= 0) {
                uri = new URI(reqUri.getScheme(), reqUri.getUserInfo(), reqUri.getHost(), reqUri.getPort(), reqPath,
                        reqUri.getQuery(), reqUri.getFragment());
            } else {
                uri = reqUri;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return uri;
    }
}
