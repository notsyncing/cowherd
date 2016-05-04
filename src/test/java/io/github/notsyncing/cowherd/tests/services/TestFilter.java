package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.models.FilterContext;
import io.github.notsyncing.cowherd.server.ServiceActionFilter;
import io.github.notsyncing.cowherd.tests.CowherdTest;

import java.util.concurrent.CompletableFuture;

public class TestFilter implements ServiceActionFilter
{
    @Override
    public CompletableFuture<Boolean> filter(FilterContext context)
    {
        CowherdTest.testFilterTriggered = true;
        return CompletableFuture.completedFuture(true);
    }
}
