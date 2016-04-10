package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.server.ServiceActionFilter;
import io.github.notsyncing.cowherd.tests.CowherdTest;

import java.util.concurrent.CompletableFuture;

public class TestFilter implements ServiceActionFilter
{
    @Override
    public CompletableFuture<Boolean> filter()
    {
        CowherdTest.testFilterTriggered = true;
        return CompletableFuture.completedFuture(true);
    }
}
