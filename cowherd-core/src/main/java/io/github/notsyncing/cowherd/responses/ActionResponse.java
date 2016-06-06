package io.github.notsyncing.cowherd.responses;

import io.github.notsyncing.cowherd.models.ActionContext;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface ActionResponse
{
    CompletableFuture writeToResponse(ActionContext context) throws IOException;
}
