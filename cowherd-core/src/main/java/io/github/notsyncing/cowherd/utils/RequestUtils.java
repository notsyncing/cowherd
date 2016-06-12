package io.github.notsyncing.cowherd.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import io.github.notsyncing.cowherd.annotations.httpmethods.*;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.exceptions.UploadOversizeException;
import io.github.notsyncing.cowherd.exceptions.ValidationFailedException;
import io.github.notsyncing.cowherd.models.UploadFileInfo;
import io.github.notsyncing.cowherd.validators.ParameterValidator;
import io.github.notsyncing.cowherd.validators.annotations.ServiceActionParameterValidator;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.HttpCookie;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class RequestUtils
{
    private static Map<Class<? extends ParameterValidator>, ParameterValidator> parameterValidators = new ConcurrentHashMap<>();

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

    public static void mapFlatParametersToList(Map<String, List<String>> params, Map.Entry<String, String> e)
    {
        if (params.containsKey(e.getKey())) {
            params.get(e.getKey()).add(e.getValue());
        } else {
            List<String> l = new ArrayList<>();
            l.add(e.getValue());

            params.put(e.getKey(), l);
        }
    }

    public static CompletableFuture<Map<String, List<String>>> extractRequestParameters(HttpServerRequest req,
                                                                                        Map<String, List<String>> additionalParams)
    {
        CompletableFuture<Map<String, List<String>>> future = new CompletableFuture<>();
        Map<String, List<String>> params = new HashMap<>(additionalParams);

        req.params().forEach(e -> mapFlatParametersToList(params, e));

        if (req.isExpectMultipart()) {
            req.endHandler(r -> {
                req.formAttributes().forEach(e -> mapFlatParametersToList(params, e));
                future.complete(params);
            });
        } else {
            if (checkIfRequestHasBody(req)) {
                req.bodyHandler(body -> {
                    QueryStringDecoder decoder = new QueryStringDecoder(body.toString(), false);
                    params.putAll(decoder.parameters());

                    future.complete(params);
                });
            } else {
                future.complete(params);
            }
        }

        return future;
    }

    public static CompletableFuture<List<UploadFileInfo>> extractUploads(HttpServerRequest req)
    {
        List<UploadFileInfo> uploads = new ArrayList<>();
        List<CompletableFuture<Void>> uploadFutures = new ArrayList<>();

        req.uploadHandler(upload -> {
            CompletableFuture<Void> uf = new CompletableFuture<>();

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

            uploadFutures.add(uf);

            UploadFileInfo info = new UploadFileInfo();
            info.setFile(f);
            info.setFilename(upload.filename());
            info.setParameterName(upload.name());

            uploads.add(info);
        });

        return CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[uploadFutures.size()]))
                .thenApply(t -> uploads);
    }

    public static Object[] convertParameterListToMethodParameters(Method method, HttpServerRequest req,
                                                                  Map<String, List<String>> params,
                                                                  List<HttpCookie> cookies,
                                                                  List<UploadFileInfo> uploads,
                                                                  Object... otherParameters) throws IllegalAccessException, InstantiationException, ValidationFailedException
    {
        List<Object> targetParams = new ArrayList<>();
        Parameter[] pl = method.getParameters();
        JSONObject jsonParams = null;

        if (params.containsKey("__json__")) {
            String json = params.get("__json__").get(0);
            jsonParams = JSON.parseObject(json);
        }

        for (int i = 0; i < pl.length; i++) {
            Parameter p = pl[i];

            if (!p.isNamePresent()) {
                throw new RuntimeException("Parameter #" + i + " <" + p.getType() + "> of method " + method.getName() +
                        " has no name present, you must compile your program with -parameters!");
            }

            if (p.getName().equals("__parameters__")) {
                targetParams.add(params);
            } else if (p.getName().equals("__uploads__")) {
                targetParams.add(uploads);
            } else if (p.getName().equals("__cookies__")) {
                targetParams.add(cookies);
            } else if (p.getType() == UploadFileInfo.class) {
                if (uploads != null) {
                    Optional<UploadFileInfo> ufi = uploads.stream()
                            .filter(u -> u.getParameterName().equals(p.getName()))
                            .findFirst();

                    targetParams.add(ufi.isPresent() ? ufi.get() : null);
                } else {
                    targetParams.add(null);
                }
            } else if (p.getType() == UploadFileInfo[].class) {
                if (uploads != null) {
                    targetParams.add(uploads.toArray(new UploadFileInfo[uploads.size()]));
                } else {
                    targetParams.add(null);
                }
            } else if (p.getType() == HttpServerRequest.class) {
                targetParams.add(req);
            } else if (p.getType() == HttpServerResponse.class) {
                targetParams.add(req.response());
            } else if (p.getType() == HttpCookie.class) {
                if (cookies != null) {
                    String name = p.getName();
                    HttpCookie cookie = cookies.stream()
                            .filter(c -> c.getName().equals(name))
                            .findFirst()
                            .orElse(null);

                    targetParams.add(cookie);
                } else {
                    targetParams.add(null);
                }
            } else if (p.getType().isEnum()) {
                if (params.containsKey(p.getName())) {
                    String value = params.get(p.getName()).get(0);
                    targetParams.add(p.getType().getEnumConstants()[Integer.parseInt(value)]);
                } else {
                    targetParams.add(null);
                }
            } else {
                if ((jsonParams != null) && (jsonParams.containsKey(p.getName()))) {
                    targetParams.add(jsonParams.getObject(p.getName(), pl[i].getType()));
                } else {
                    Object o = Stream.of(otherParameters)
                            .filter(op -> p.getType().isAssignableFrom(op.getClass()))
                            .findFirst()
                            .orElse(null);

                    if (o != null) {
                        targetParams.add(o);
                    } else {
                        if (!params.containsKey(p.getName())) {
                            targetParams.add(null);
                        } else {
                            List<String> values = params.get(p.getName());

                            if (values.size() <= 0) {
                                targetParams.add(null);
                                continue;
                            }

                            if (p.getType().isArray()) {
                                targetParams.add(TypeUtils.stringListToArrayType(p.getType().getComponentType(), values));
                            } else {
                                targetParams.add(TypeUtils.stringToType(p.getType(), values.get(0)));
                            }
                        }
                    }
                }
            }
        }

        JSONObject complexParams = new JSONObject();

        params.forEach((k, v) -> {
            if ((k.contains(".")) || (k.contains("["))) {
                complexKeyToJsonObject(complexParams, k, v);
            }
        });

        for (int i = 0; i < targetParams.size(); i++) {
            if (targetParams.get(i) != null) {
                continue;
            }

            Parameter p = pl[i];

            if (!complexParams.containsKey(p.getName())) {
                continue;
            }

            Object o = complexParams.get(p.getName());

            if ((o instanceof JSONArray) || (o instanceof JSONObject)) {
                targetParams.set(i, JSON.parseObject(o.toString(), p.getParameterizedType()));
            } else {
                targetParams.set(i, o);
            }
        }

        validateMethodParameters(method, targetParams);

        return targetParams.toArray(new Object[targetParams.size()]);
    }

    private static void validateMethodParameters(Method method, List<Object> targetParams) throws InstantiationException, IllegalAccessException, ValidationFailedException
    {
        for (Parameter p : method.getParameters()) {
            if ((p.getAnnotations() == null) || (p.getAnnotations().length <= 0)) {
                continue;
            }

            for (Annotation a : p.getAnnotations()) {
                if (!a.annotationType().isAnnotationPresent(ServiceActionParameterValidator.class)) {
                    continue;
                }

                ServiceActionParameterValidator validatorAnno = a.annotationType().getAnnotation(ServiceActionParameterValidator.class);

                if (!parameterValidators.containsKey(validatorAnno.value())) {
                    parameterValidators.put(validatorAnno.value(), validatorAnno.value().newInstance());
                }

                ParameterValidator validator = parameterValidators.get(validatorAnno.value());
                Object value = targetParams.get(targetParams.size() - 1);

                if (!validator.validate(p, a, value)) {
                    throw new ValidationFailedException(p, validator, a, value);
                }
            }
        }
    }

    public static void complexKeyToJsonObject(JSONObject hubObject, String key, List<String> values)
    {
        String[] keySections = key.split("\\.");

        for (int i = 0; i < values.size(); i++) {
            Object currObj;
            Object prevObj = hubObject;

            for (int j = 0; j < keySections.length; j++) {
                String keyPart = keySections[j];
                String name = keyPart.replaceAll("\\[\\]", "");
                boolean currentKeyIsArray = keyPart.endsWith("[]");

                if (currentKeyIsArray) {
                    currObj = new JSONArray();
                } else {
                    currObj = new JSONObject();
                }

                if (prevObj instanceof JSONArray) {
                    JSONArray prevArray = (JSONArray) prevObj;

                    if (currentKeyIsArray) {
                        if (j >= keySections.length - 1) {
                            prevArray.add(values.get(i));
                        }
                    } else {
                        if (i < prevArray.size()) {
                            currObj = prevArray.get(i);
                        } else {
                            prevArray.add(currObj);
                        }

                        ((JSONObject)currObj).put(name, values.get(i));
                    }
                } else {
                    JSONObject prevObject = (JSONObject) prevObj;

                    if (j >= keySections.length - 1) {
                        if (!currentKeyIsArray) {
                            prevObject.put(name, values.get(i));
                        } else {
                            if (!prevObject.containsKey(name)) {
                                prevObject.put(name, currObj);
                            }

                            JSONArray a = prevObject.getJSONArray(name);
                            a.add(values.get(i));
                        }
                    } else {
                        if (!prevObject.containsKey(name)) {
                            prevObject.put(name, currObj);
                        } else {
                            currObj = prevObject.get(name);
                        }
                    }
                }

                prevObj = currObj;
            }
        }
    }

    public static List<HttpCookie> parseHttpCookies(HttpServerRequest request)
    {
        String cookieHeader = request.getHeader("Cookie");
        List<HttpCookie> cookies = null;

        if (cookieHeader != null) {
            cookies = CookieUtils.parseServerCookies(cookieHeader);
        }

        return cookies;
    }
}
