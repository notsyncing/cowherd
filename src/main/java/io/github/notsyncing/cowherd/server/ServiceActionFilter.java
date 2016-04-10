package io.github.notsyncing.cowherd.server;

import java.util.concurrent.CompletableFuture;

public interface ServiceActionFilter
{
    CompletableFuture<Boolean> filter();
}
