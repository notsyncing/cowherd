package io.github.notsyncing.cowherd.utils;

import io.github.notsyncing.cowherd.annotations.httpmethods.*;
import io.github.notsyncing.cowherd.models.SimpleURI;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

import java.lang.reflect.Method;
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

    public static SimpleURI reduceUri(String path, String absoluteUri)
    {
        String reqPath = StringUtils.stripSameCharAtStringHeader(path, '/');
        int homeCharPos = reqPath.lastIndexOf("~");

        if (homeCharPos >= 0) {
            reqPath = reqPath.substring(homeCharPos + 1);
        }

        SimpleURI uri = null;

        try {
            uri = new SimpleURI(absoluteUri);

            if (homeCharPos >= 0) {
                uri.setPath(reqPath);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return uri;
    }

    public static SimpleURI resolveUriFromRequest(HttpServerRequest request)
    {
        return reduceUri(request.path(), request.absoluteURI());
    }
}
