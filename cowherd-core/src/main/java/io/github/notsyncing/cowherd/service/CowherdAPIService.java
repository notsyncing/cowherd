package io.github.notsyncing.cowherd.service;

import io.github.notsyncing.cowherd.annotations.AsEnum;
import io.github.notsyncing.cowherd.annotations.ContentType;
import io.github.notsyncing.cowherd.annotations.Exported;
import io.github.notsyncing.cowherd.annotations.ExposeAsEnum;
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpAnyMethod;
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpGet;
import io.github.notsyncing.cowherd.models.CowherdServiceInfo;
import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.models.UploadFileInfo;
import io.github.notsyncing.cowherd.utils.RequestUtils;
import io.github.notsyncing.cowherd.utils.RouteUtils;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.HttpCookie;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CowherdAPIService extends CowherdService
{
    private static Map<String, Date> injectServiceScriptGenerationTimes = new ConcurrentHashMap<>();

    @HttpAnyMethod
    @Exported
    public CompletableFuture gateway(String __service__, String __action__, HttpServerRequest request,
                                     List<Pair<String, String>> __parameters__, List<HttpCookie> __cookies__,
                                     List<UploadFileInfo> __uploads__)
    {
        return delegateTo(__service__, __action__, request, __parameters__, __cookies__,  __uploads__);
    }

    @HttpGet
    @Exported
    @ContentType("text/javascript")
    public String injectServices(String base, String service, HttpServerRequest request) throws IOException, ClassNotFoundException
    {
        if (StringUtils.isEmpty(service)) {
            service = "ALL";
        }

        if ((!StringUtils.isEmpty(request.getHeader("If-Modified-Since"))) && (injectServiceScriptGenerationTimes.containsKey(service))) {
            request.response().putHeader("Last-Modified",
                    StringUtils.dateToHttpDateString(injectServiceScriptGenerationTimes.get(service)));
            request.response().setStatusCode(304).end();
            return null;
        }

        injectServiceScriptGenerationTimes.put(service, new Date());
        request.response().putHeader("Last-Modified", StringUtils.dateToHttpDateString(injectServiceScriptGenerationTimes.get(service)));

        if (StringUtils.isEmpty(base)) {
            base = "~/";
        }

        String js = "(function () {\n";
        Set<Class<?>> generatedEnumClasses = new HashSet<>();

        for (CowherdServiceInfo info : ServiceManager.getServices()) {
            if ((!"ALL".equals(service)) && (!info.getName().equals(service))) {
                continue;
            }

            if (!StringUtils.isEmpty(info.getNamespace())) {
                js += "if (!window." + info.getNamespace() + ") {\n";
                js += "window." + info.getNamespace() + " = {};\n";
                js += "}\n";
                js += "window." + info.getNamespace() + "." + info.getName() + " = {};\n";
            } else {
                js += "window." + info.getName() + " = {};\n";
            }

            for (Method m : info.getServiceClass().getMethods()) {
                if (!m.isAnnotationPresent(Exported.class)) {
                    continue;
                }

                Exported exportedInfo = m.getAnnotation(Exported.class);

                if (!exportedInfo.api()) {
                    continue;
                }

                js += generateMethodReturnEnum(info, m, generatedEnumClasses);
                js += generateMethodParameterEnums(info, m, generatedEnumClasses);
                js += generateMethodCall(base, info, m);
            }
        }

        js += "})();\n";
        return js;
    }

    private String getGenericReturnTypeName(Method m)
    {
        String fullName = m.getGenericReturnType().getTypeName();

        if (!fullName.contains("<")) {
            return fullName;
        }

        return fullName.substring(fullName.indexOf("<") + 1, fullName.length() - 1);
    }

    @SuppressWarnings("unchecked")
    private String generateMethodReturnEnum(CowherdServiceInfo info, Method m,
                                            Set<Class<?>> generatedEnumClasses) throws ClassNotFoundException
    {
        String js = "";
        Class<?> e = null;

        if (Enum.class.isAssignableFrom(m.getReturnType())) {
            e = m.getReturnType();
        } else if (m.getReturnType().isAnnotationPresent(ExposeAsEnum.class)) {
            e = m.getReturnType();
        } else {
            String n = getGenericReturnTypeName(m);

            if (n.contains("<")) {
                return "";
            }

            if ((!n.equals(m.getReturnType().getName())) && (Enum.class.isAssignableFrom(Class.forName(n)))) {
                e = Class.forName(getGenericReturnTypeName(m));
            }
        }

        if (e != null) {
            if (generatedEnumClasses.contains(e)) {
                return "";
            }

            if (!StringUtils.isEmpty(info.getNamespace())) {
                js += "window." + info.getNamespace() + "." + e.getSimpleName();
            } else {
                js += "window." + e.getSimpleName();
            }

            js += " = {\n";

            if (Enum.class.isAssignableFrom(e)) {
                js += Stream.of(e.getEnumConstants())
                        .map(n -> (Enum)n)
                        .map(n -> n.name() + ": " + n.ordinal())
                        .collect(Collectors.joining(",\n"));
            } else if (e.isAnnotationPresent(ExposeAsEnum.class)) {
                js += Stream.of(e.getFields())
                        .filter(f -> f.isAnnotationPresent(AsEnum.class))
                        .map(f -> {
                            try {
                                return f.getName() + ": " + f.get(null).toString();
                            } catch (IllegalAccessException e1) {
                                e1.printStackTrace();
                            }

                            return "";
                        })
                        .collect(Collectors.joining(",\n"));
            }

            js += "\n};\n";
        }

        generatedEnumClasses.add(e);

        return js;
    }

    @SuppressWarnings("unchecked")
    private String generateMethodParameterEnums(CowherdServiceInfo info, Method m,
                                                Set<Class<?>> generatedEnumClasses) throws ClassNotFoundException
    {
        String js = "";
        Class<?> e;

        for (Parameter p : m.getParameters()) {
            if ((!p.getType().isEnum()) && (!p.getType().isAnnotationPresent(ExposeAsEnum.class))) {
                continue;
            }

            e = p.getType();

            if (generatedEnumClasses.contains(e)) {
                continue;
            }

            if (!StringUtils.isEmpty(info.getNamespace())) {
                js += "window." + info.getNamespace() + "." + e.getSimpleName();
            } else {
                js += "window." + e.getSimpleName();
            }

            js += " = {\n";

            if (Enum.class.isAssignableFrom(e)) {
                js += Stream.of(e.getEnumConstants())
                        .map(n -> (Enum)n)
                        .map(n -> n.name() + ": " + n.ordinal())
                        .collect(Collectors.joining(",\n"));
            } else if (e.isAnnotationPresent(ExposeAsEnum.class)) {
                js += Stream.of(e.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(AsEnum.class))
                        .map(f -> {
                            try {
                                return f.getName() + ": " + f.get(null).toString();
                            } catch (IllegalAccessException e1) {
                                e1.printStackTrace();
                            }

                            return "";
                        })
                        .collect(Collectors.joining(",\n"));
            }

            js += "\n};\n";

            generatedEnumClasses.add(e);
        }

        return js;
    }

    private Parameter[] stripMethodParameters(Method m)
    {
        Parameter[] params = Stream.of(m.getParameters())
                .filter(p -> (!p.getType().equals(HttpCookie.class))
                        && (!p.getType().equals(HttpServerRequest.class))
                        && (!p.getType().equals(HttpServerResponse.class))
                        && (!p.getType().equals(UploadFileInfo.class))
                        && (!UploadFileInfo.class.equals(p.getType().getComponentType()))
                        && (!p.getName().equals("__json__"))
                        && (!p.getName().equals("__parameters__"))
                        && (!p.getName().equals("__uploads__"))
                        && (!p.getName().equals("__cookies__")))
                .toArray(Parameter[]::new);

        if (params == null) {
            params = new Parameter[0];
        }

        return params;
    }

    private String generateMethodCall(String base, CowherdServiceInfo info, Method m)
    {
        String js = "";

        if (!StringUtils.isEmpty(info.getNamespace())) {
            js += "window." + info.getNamespace() + "." + info.getName();
        } else {
            js += "window." + info.getName();
        }

        js += "." + m.getName() + " = function (";
        Parameter[] params = stripMethodParameters(m);

        if (params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                js += RequestUtils.getParameterName(m, params[i], i) + ", ";
            }

            js = js.substring(0, js.length() - 2);
        }

        js += ") {\n";
        js += "return MagicForm.ajax({\n";
        js += "url: '" + base + "api/gateway',\n";
        js += "method: '" + RouteUtils.getActionHttpMethodString(m) + "',\n";
        js += "data: {\n";
        js += "__service__: '" + info.getFullName() + "',\n";
        js += "__action__: '" + m.getName() + "'";

        if (params.length > 0) {
            js += ",\n";

            for (int i = 0; i < params.length; i++) {
                String p = RequestUtils.getParameterName(m, params[i], i);

                if (List.class.isAssignableFrom(params[i].getType())) {
                    p += "[]";
                }

                js += p + ": " + p + ",\n";
            }

            js = js.substring(0, js.length() - 2);
        }

        js += "\n";
        js += "}\n";
        js += "}).then(function (resp) { return resp.response; });\n";
        js += "};\n";

        return js;
    }
}
