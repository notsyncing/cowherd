package io.github.notsyncing.cowherd.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import io.github.notsyncing.cowherd.annotations.httpmethods.*;
import io.github.notsyncing.cowherd.commons.AlternativeCookieHeaderConfig;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.exceptions.UploadOversizeException;
import io.github.notsyncing.cowherd.exceptions.ValidationFailedException;
import io.github.notsyncing.cowherd.models.ActionContext;
import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.models.RequestContext;
import io.github.notsyncing.cowherd.models.UploadFileInfo;
import io.github.notsyncing.cowherd.server.CowherdLogger;
import io.github.notsyncing.cowherd.utils.deserializers.Jdk8NullableDateCodec;
import io.github.notsyncing.cowherd.validators.ParameterValidator;
import io.github.notsyncing.cowherd.validators.annotations.ServiceActionParameterValidator;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.HttpCookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.notsyncing.cowherd.utils.CookieUtils.parseServerCookies;

public class RequestUtils
{
    private static Map<Class<? extends ParameterValidator>, ParameterValidator> parameterValidators = new ConcurrentHashMap<>();

    static {
        ParserConfig.getGlobalInstance().putDeserializer(LocalDateTime.class, new Jdk8NullableDateCodec());
        ParserConfig.getGlobalInstance().putDeserializer(LocalDate.class, new Jdk8NullableDateCodec());
    }

    public static boolean checkIfHttpMethodIsAllowedOnAction(Method m, HttpMethod httpMethod)
    {
        if (m.isAnnotationPresent(HttpAnyMethod.class)) {
            return true;
        }

        if ((httpMethod == HttpMethod.GET) && (m.isAnnotationPresent(HttpGet.class))) {
            return true;
        }

        if ((httpMethod == HttpMethod.POST) && (m.isAnnotationPresent(HttpPost.class))) {
            return true;
        }

        if ((httpMethod == HttpMethod.OPTIONS) && (m.isAnnotationPresent(HttpOptions.class))) {
            return true;
        }

        if ((httpMethod == HttpMethod.DELETE) && (m.isAnnotationPresent(HttpDelete.class))) {
            return true;
        }

        if ((httpMethod == HttpMethod.PUT) && (m.isAnnotationPresent(HttpPut.class))) {
            return true;
        }

        return (httpMethod == HttpMethod.HEAD) && (m.isAnnotationPresent(HttpHead.class));
    }

    public static boolean checkIfRequestHasBody(HttpServerRequest req)
    {
        if ((req.method() != HttpMethod.POST) && (req.method() != HttpMethod.PUT)) {
            return false;
        }

        String cl = req.getHeader("Content-Length");
        return (req.getHeader("Transfer-Encoding") != null) || ((cl != null) && (Integer.parseInt(cl) > 0));
    }

    public static String getParameterName(Method method, Parameter param, int index)
    {
        if (param.isAnnotationPresent(io.github.notsyncing.cowherd.annotations.Parameter.class)) {
            return param.getAnnotation(io.github.notsyncing.cowherd.annotations.Parameter.class).value();
        }

        if (!param.isNamePresent()) {
            CowherdLogger.getInstance(RequestUtils.class).e("Parameter #" + index + " <" +
                    param.getType() + "> of method " + method.getName() +
                    " has no name present, you must annotate it with @Parameter or compile your program with -parameters!");
        }

        return param.getName();
    }

    public static String getParameterName(Parameter param)
    {
        if (param.isAnnotationPresent(io.github.notsyncing.cowherd.annotations.Parameter.class)) {
            return param.getAnnotation(io.github.notsyncing.cowherd.annotations.Parameter.class).value();
        }

        return param.getName();
    }

    public static Object[] convertParameterListToMethodParameters(ActionContext context,
                                                                  List<Pair<String, String>> params,
                                                                  List<HttpCookie> cookies,
                                                                  List<UploadFileInfo> uploads,
                                                                  Object... otherParameters) throws IllegalAccessException, InstantiationException, ValidationFailedException
    {
        Method method = context.getActionMethod();
        HttpServerRequest req = context.getRequest();

        Parameter[] pl = method.getParameters();

        if ((pl == null) || (pl.length <= 0)) {
            return null;
        }

        Object[] targetParams = new Object[pl.length];
        List<Parameter> methodParams = Arrays.asList(pl);
        Map<String, Parameter> methodParamMap = new HashMap<>();

        for (int i = 0; i < methodParams.size(); i++) {
            Parameter p = methodParams.get(i);
            methodParamMap.put(RequestUtils.getParameterName(method, p, i), p);
        }

        JSONObject jsonParams = null;
        String bodyParam = null;
        List<Pair<String, String>> complexParamPairs = new ArrayList<>();

        for (Pair<String, String> reqParam : params) {
            if (reqParam.getKey().equals("__json__")) {
                jsonParams = JSON.parseObject(reqParam.getValue());
            } else if (reqParam.getKey().equals("__body__")) {
                bodyParam = reqParam.getValue();
            }

            if ((reqParam.getKey().contains(".")) || (reqParam.getKey().contains("["))) {
                complexParamPairs.add(reqParam);
                continue;
            }

            if (!methodParamMap.containsKey(reqParam.getKey())) {
                continue;
            }

            Parameter methodParam = methodParamMap.get(reqParam.getKey());
            int methodParamIndex = methodParams.indexOf(methodParam);

            if (methodParam.getType().isEnum()) {
                int e = Integer.parseInt(reqParam.getValue());

                if (e < 0) {
                    targetParams[methodParamIndex] = null;
                } else {
                    targetParams[methodParamIndex] = methodParam.getType().getEnumConstants()[e];
                }
            } else if (methodParam.getType().isArray()) {
                List<String> values = params.stream()
                        .filter(p -> p.getKey().equals(reqParam.getKey()))
                        .map(Pair::getValue)
                        .collect(Collectors.toList());

                targetParams[methodParamIndex] = TypeUtils.stringListToArrayType(methodParam.getType().getComponentType(), values);
            } else {
                targetParams[methodParamIndex] = TypeUtils.stringToType(methodParam.getType(), reqParam.getValue());
            }
        }

        for (int i = 0; i < methodParams.size(); i++) {
            Parameter methodParam = methodParams.get(i);
            String methodParamName = getParameterName(method, methodParam, i);

            if (methodParamName.equals("__parameters__")) {
                targetParams[i] = params;
            } else if (methodParamName.equals("__uploads__")) {
                targetParams[i] = uploads;
            } else if (methodParamName.equals("__cookies__")) {
                targetParams[i] = cookies;
            } else if (methodParamName.equals("__body__")) {
                targetParams[i] = bodyParam;
            } else if (methodParam.getType() == UploadFileInfo.class) {
                if (uploads != null) {
                    Optional<UploadFileInfo> ufi = uploads.stream()
                            .filter(u -> u.getParameterName().equals(methodParamName))
                            .findFirst();

                    targetParams[i] = ufi.isPresent() ? ufi.get() : null;
                } else {
                    targetParams[i] = null;
                }
            } else if (methodParam.getType() == UploadFileInfo[].class) {
                if (uploads != null) {
                    targetParams[i] = uploads.toArray(new UploadFileInfo[uploads.size()]);
                } else {
                    targetParams[i] = null;
                }
            } else if (methodParam.getType() == ActionContext.class) {
                targetParams[i] = context;
            } else if (methodParam.getType() == HttpServerRequest.class) {
                targetParams[i] = req;
            } else if (methodParam.getType() == HttpServerResponse.class) {
                targetParams[i] = req.response();
            } else if (methodParam.getType() == HttpCookie.class) {
                if (cookies != null) {
                    HttpCookie cookie = cookies.stream()
                            .filter(c -> c.getName().equals(methodParamName))
                            .findFirst()
                            .orElse(null);

                    targetParams[i] = cookie;
                } else {
                    targetParams[i] = null;
                }
            } else if ((jsonParams != null) && (jsonParams.containsKey(methodParamName))) {
                targetParams[i] = jsonParams.getObject(methodParamName, pl[i].getType());
            } else if (otherParameters != null) {
                Object o = Stream.of(otherParameters)
                        .filter(op -> (op != null) && (methodParam.getType().isAssignableFrom(op.getClass())))
                        .findFirst()
                        .orElse(null);

                if (o != null) {
                    targetParams[i] = o;
                }
            }
        }

        if (complexParamPairs.size() > 0) {
            JSONObject complexParams = new JSONObject();

            complexKeyToJsonObject(complexParams, complexParamPairs);

            for (int i = 0; i < targetParams.length; i++) {
                if (targetParams[i] != null) {
                    continue;
                }

                Parameter p = pl[i];
                String pn = getParameterName(p);

                if (!complexParams.containsKey(pn)) {
                    continue;
                }

                Object o = complexParams.get(pn);

                if ((o instanceof JSONArray) || (o instanceof JSONObject)) {
                    targetParams[i] = JSON.parseObject(o.toString(), p.getParameterizedType());
                } else {
                    targetParams[i] = o;
                }
            }
        }

        validateMethodParameters(method, targetParams);

        return targetParams;
    }

    private static void validateMethodParameters(Method method, Object[] targetParams) throws InstantiationException, IllegalAccessException, ValidationFailedException
    {
        if (method.getParameters() == null) {
            return;
        }

        for (int i = 0; i < method.getParameters().length; i++) {
            Parameter p = method.getParameters()[i];

            if ((p.getAnnotations() != null) && (p.getAnnotations().length > 0)) {
                for (Annotation a : p.getAnnotations()) {
                    if (!a.annotationType().isAnnotationPresent(ServiceActionParameterValidator.class)) {
                        continue;
                    }

                    ServiceActionParameterValidator validatorAnno = a.annotationType().getAnnotation(ServiceActionParameterValidator.class);

                    if (!parameterValidators.containsKey(validatorAnno.value())) {
                        parameterValidators.put(validatorAnno.value(), validatorAnno.value().newInstance());
                    }

                    ParameterValidator validator = parameterValidators.get(validatorAnno.value());
                    Object value = targetParams[i];

                    if (!validator.validate(p, a, value)) {
                        throw new ValidationFailedException(p, validator, a, value);
                    }

                    targetParams[i] = validator.filter(p, a, value);
                }
            }

            if ((p.getType().isPrimitive()) && (targetParams[i] == null)) {
                throw new IllegalAccessException("Parameter #" + i + " '" + p.getName() + "' <" + p.getType() +
                        "> of method " + method.toString() + " is primitive, but received an null value!");
            }
        }
    }

    public static void complexKeyToJsonObject(JSONObject hubObject, List<Pair<String, String>> pairs)
    {
        if (hubObject == null) {
            return;
        }

        if ((pairs == null) || (pairs.size() <= 0)) {
            return;
        }

        List<Pair<String, String>> jsonPaths = new ArrayList<>();
        Map<String, Integer> arrayCounters = new HashMap<>();
        Map<String, String> arrayNextKey = new HashMap<>();

        for (Pair<String, String> p : pairs) {
            if (!p.getKey().contains("[]")) {
                jsonPaths.add(new Pair<>(p.getKey(), p.getValue()));
            } else {
                int lastArrayIndex = 0;
                String lastAddedArrayPath = null;
                boolean incArrayIndex = false;

                while (lastArrayIndex >= 0) {
                    lastArrayIndex = p.getKey().indexOf("[]", lastArrayIndex + 1);

                    if (lastArrayIndex < 0) {
                        break;
                    }

                    String currArrayPath = p.getKey().substring(0, lastArrayIndex);

                    if (!arrayCounters.containsKey(currArrayPath)) {
                        arrayCounters.put(currArrayPath, 0);
                        lastAddedArrayPath = currArrayPath;
                    }

                    String nextKey = p.getKey().substring(lastArrayIndex + 2);
                    nextKey = nextKey.substring(nextKey.indexOf(".") + 1);

                    if (nextKey.contains("[")) {
                        nextKey = nextKey.substring(nextKey.indexOf("[") + 1);
                    }

                    if (!arrayNextKey.containsKey(currArrayPath)) {
                        arrayNextKey.put(currArrayPath, nextKey);
                    } else if (arrayNextKey.get(currArrayPath).equals(nextKey)) {
                        incArrayIndex = true;
                    }
                }

                lastArrayIndex = p.getKey().lastIndexOf("[]");
                String arrayPath = p.getKey().substring(0, lastArrayIndex);

                if ((!arrayPath.equals(lastAddedArrayPath)) && (incArrayIndex)) {
                    arrayCounters.replace(arrayPath, arrayCounters.get(arrayPath) + 1);
                }

                lastArrayIndex = 0;
                String finalArrayPath = p.getKey();

                while (lastArrayIndex >= 0) {
                    lastArrayIndex = finalArrayPath.indexOf("[]", lastArrayIndex + 1);

                    if (lastArrayIndex < 0) {
                        break;
                    }

                    String currArrayPath = finalArrayPath.substring(0, lastArrayIndex).replaceAll("\\[\\d+\\]", "[]");
                    int currArrayIndex = arrayCounters.get(currArrayPath);
                    finalArrayPath = finalArrayPath.substring(0, lastArrayIndex) + "[" + currArrayIndex + "]" +
                            finalArrayPath.substring(lastArrayIndex + 2);
                }

                jsonPaths.add(new Pair<>(finalArrayPath, p.getValue()));
            }
        }

        fillJSONObjectByPaths(hubObject, jsonPaths);
    }

    private static void fillJSONObjectByPaths(JSONObject hubObject, List<Pair<String, String>> jsonPaths)
    {
        for (Pair<String, String> path : jsonPaths) {
            String[] sections = path.getKey().split("\\.");
            JSON parentObj = hubObject;
            Object value = path.getValue();

            if (StringUtils.isInteger((String)value)) {
                value = Integer.parseInt((String)value);
            }

            for (int i = 0; i < sections.length; i++) {
                String keyName = sections[i].replaceAll("\\[\\d+\\]", "");
                boolean currKeyIsArray = sections[i].contains("[");

                if (!currKeyIsArray) {
                    if (i < sections.length - 1) {
                        JSONObject o = new JSONObject();

                        if (parentObj instanceof JSONArray) {
                            ((JSONArray)parentObj).add(o);
                        } else if (parentObj instanceof JSONObject) {
                            JSONObject parentObject = (JSONObject)parentObj;

                            if (!parentObject.containsKey(keyName)) {
                                parentObject.put(keyName, o);
                            } else {
                                o = parentObject.getJSONObject(keyName);
                            }
                        }

                        parentObj = o;
                    } else {
                        if (parentObj instanceof JSONArray) {
                            JSONObject o = new JSONObject();
                            o.put(keyName, value);
                            ((JSONArray)parentObj).add(o);
                        } else if (parentObj instanceof JSONObject) {
                            ((JSONObject)parentObj).put(keyName, value);
                        }
                    }
                } else {
                    int indexStart = sections[i].indexOf("[");
                    int indexEnd = sections[i].indexOf("]");
                    int currArrayIndex = Integer.parseInt(sections[i].substring(indexStart + 1, indexEnd));

                    if (i < sections.length - 1) {
                        if (parentObj instanceof JSONArray) {
                            JSONArray parentArray = (JSONArray) parentObj;
                            JSONObject o;

                            if (currArrayIndex >= parentArray.size()) {
                                o = new JSONObject();
                                o.put(keyName, new JSONArray());
                                o.getJSONArray(keyName).add(new JSONObject());
                                parentObj = (JSON)o.getJSONArray(keyName).get(0);
                            } else {
                                parentObj = parentArray.getJSONObject(currArrayIndex).getJSONArray(keyName);
                            }
                        } else if (parentObj instanceof JSONObject) {
                            JSONObject parentObject = (JSONObject)parentObj;
                            JSONArray currArray;

                            if (!parentObject.containsKey(keyName)) {
                                parentObject.put(keyName, new JSONArray());
                            }

                            currArray = ((JSONObject) parentObj).getJSONArray(keyName);

                            if (currArrayIndex >= currArray.size()) {
                                currArray.add(new JSONObject());
                            }

                            parentObj = currArray.getJSONObject(currArrayIndex);
                        }
                    } else {
                        if (parentObj instanceof JSONArray) {

                        } else if (parentObj instanceof JSONObject) {
                            JSONObject parentObject = (JSONObject)parentObj;
                            JSONArray currArray;

                            if (!parentObject.containsKey(keyName)) {
                                parentObject.put(keyName, new JSONArray());
                            }

                            currArray = ((JSONObject) parentObj).getJSONArray(keyName);

                            if (currArrayIndex >= currArray.size()) {
                                currArray.add(value);
                            } else {
                                currArray.set(currArrayIndex, value);
                            }
                        }
                    }
                }
            }
        }
    }

    public static List<HttpCookie> parseHttpCookies(HttpServerRequest request)
    {
        List<HttpCookie> cookies = new ArrayList<>();
        String cookieHeader = request.getHeader("Cookie");
        String altCookieHeader = null;

        AlternativeCookieHeaderConfig ch = CowherdConfiguration.getAlternativeCookieHeaders();

        if (ch != null) {
            if (((!StringUtils.isEmpty(ch.getOnlyOn()))
                    || ("true".equals(request.getHeader(ch.getOnlyOn()))))
                    && (!StringUtils.isEmpty(ch.getCookie()))) {
                altCookieHeader = request.getHeader(ch.getCookie());
            }
        }

        if (altCookieHeader != null) {
            cookies.addAll(parseServerCookies(altCookieHeader));
        }

        if (cookieHeader != null) {
            CookieUtils.parseServerCookies(cookieHeader).stream()
                    .filter(c -> !cookies.stream().anyMatch(c2 -> c2.getName().equals(c.getName())))
                    .forEach(cookies::add);
        }

        return cookies;
    }

    public static CompletableFuture<RequestContext> toRequestContext(HttpServerRequest request)
    {
        CompletableFuture<RequestContext> future = new CompletableFuture<>();
        CompletableFuture<Void> bodyFuture = new CompletableFuture<>();
        List<CompletableFuture<Void>> uploadFutures = new ArrayList<>();
        Buffer bodyBuffer = Buffer.buffer();

        RequestContext context = new RequestContext();
        context.setRequest(request);
        context.setResponse(request.response());
        context.setMethod(request.method());
        context.setPath(request.path());
        context.setHeaders(request.headers());

        request.params().forEach(e -> context.getParameters().add(new Pair<>(e.getKey(), e.getValue())));

        request.setExpectMultipart(true);

        if (checkIfRequestHasBody(request)) {
            request.handler(b -> {
                if (!request.isExpectMultipart()) {
                    bodyBuffer.appendBuffer(b);
                }
            });
        } else {
            bodyFuture.complete(null);
        }

        request.uploadHandler(upload -> {
            CompletableFuture<Void> uf = new CompletableFuture<>();
            uploadFutures.add(uf);

            if (upload.size() > CowherdConfiguration.getMaxUploadFileSize()) {
                uf.completeExceptionally(new UploadOversizeException(upload.filename()));
                return;
            }

            File f;

            try {
                f = new File(CowherdConfiguration.getUploadCacheDir().toFile(), UUID.randomUUID().toString());
            } catch (Exception e) {
                uf.completeExceptionally(e);
                return;
            }

            upload.streamToFileSystem(f.getAbsolutePath())
                    .endHandler(uf::complete);

            UploadFileInfo info = new UploadFileInfo();
            info.setFile(f);
            info.setFilename(upload.filename());
            info.setParameterName(upload.name());

            context.getUploads().add(info);
        });

        request.exceptionHandler(future::completeExceptionally);

        request.endHandler(r -> {
            if (request.isExpectMultipart()) {
                request.formAttributes()
                        .forEach(e -> context.getParameters().add(new Pair<>(e.getKey(), e.getValue())));
            }

            if (!bodyFuture.isDone()) {
                String bodyStr = bodyBuffer.toString();

                context.getParameters().addAll(StringUtils.parseQueryString(bodyStr));
                context.getParameters().add(new Pair<>("__body__", bodyStr));

                bodyFuture.complete(null);
            }

            uploadFutures.add(bodyFuture);
            CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0]))
                    .thenAccept(v -> future.complete(context));
        });

        return future;
    }

    public static void putCookie(HttpServerRequest request, HttpCookie cookie)
    {
        String cookieString = CookieUtils.cookieToString(cookie);
        request.response().headers().add("Set-Cookie", cookieString);

        AlternativeCookieHeaderConfig ch = CowherdConfiguration.getAlternativeCookieHeaders();

        if ((ch != null) && ((StringUtils.isEmpty(ch.getOnlyOn()))
                || ("true".equals(request.getHeader(ch.getOnlyOn()))))
                && (!StringUtils.isEmpty(ch.getSetCookie()))) {
            request.response().headers().add(ch.getSetCookie(), cookieString);

            if (request.response().headers().contains("Access-Control-Allow-Origin")) {
                request.response().headers().add("Access-Control-Expose-Headers", ch.getSetCookie());
            }
        }
    }

    public static void putCookies(HttpServerRequest request, HttpCookie... cookies)
    {
        if (cookies == null) {
            return;
        }

        for (HttpCookie c : cookies) {
            putCookie(request, c);
        }
    }
}
