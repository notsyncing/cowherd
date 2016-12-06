package io.github.notsyncing.cowherd.utils;

import io.github.notsyncing.cowherd.annotations.httpmethods.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

public class RouteUtils
{
    private static String httpMethodToString(HttpMethod m)
    {
        switch (m) {
            case GET:
                return "get";
            case POST:
                return "post";
            case PUT:
                return "put";
            case DELETE:
                return "delete";
            case HEAD:
                return "head";
            case OPTIONS:
                return "options";
            case OTHER:
                return "other";
            case CONNECT:
                return "connect";
            case PATCH:
                return "patch";
            case TRACE:
                return "trace";
            default:
                return "unknown";
        }
    }

    public static String getActionHttpMethodString(Method m)
    {
        if (m.isAnnotationPresent(HttpAnyMethod.class)) {
            HttpAnyMethod a = m.getAnnotation(HttpAnyMethod.class);
            return httpMethodToString(a.prefer());
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
