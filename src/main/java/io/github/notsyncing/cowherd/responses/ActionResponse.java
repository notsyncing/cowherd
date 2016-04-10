package io.github.notsyncing.cowherd.responses;

import io.vertx.core.http.HttpServerResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface ActionResponse
{
    CompletableFuture writeToResponse(HttpServerResponse resp) throws IOException;
}
