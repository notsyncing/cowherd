package io.github.notsyncing.cowherd.utils;

import io.github.notsyncing.cowherd.annotations.httpmethods.*;
import io.github.notsyncing.cowherd.commons.GlobalStorage;
import io.github.notsyncing.cowherd.exceptions.UploadOversizeException;
import io.github.notsyncing.cowherd.models.UploadFileInfo;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RequestUtils
{
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

            if (upload.size() > GlobalStorage.getMaxUploadFileSize()) {
                uf.completeExceptionally(new UploadOversizeException(upload.filename()));
                return;
            }

            File f;

            try {
                f = new File(GlobalStorage.getUploadCacheDir().toFile(), UUID.randomUUID().toString());
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
                                                                  List<UploadFileInfo> uploads)
    {
        List<Object> targetParams = new ArrayList<>();
        Parameter[] pl = method.getParameters();

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
            } else if (p.getType() == UploadFileInfo.class) {
                Optional<UploadFileInfo> ufi = uploads.stream()
                        .filter(u -> u.getParameterName().equals(p.getName()))
                        .findFirst();

                targetParams.add(ufi.isPresent() ? ufi.get() : null);
            } else if (p.getType() == UploadFileInfo[].class) {
                targetParams.add(uploads.toArray(new UploadFileInfo[uploads.size()]));
            } else if (p.getType() == HttpServerRequest.class) {
                targetParams.add(req);
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

        return targetParams.toArray(new Object[targetParams.size()]);
    }
}
