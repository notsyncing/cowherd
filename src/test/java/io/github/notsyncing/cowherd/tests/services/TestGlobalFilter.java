package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.annotations.Global;
import io.github.notsyncing.cowherd.models.FilterContext;
import io.github.notsyncing.cowherd.server.ServiceActionFilter;
import io.github.notsyncing.cowherd.tests.CowherdTest;

import java.util.concurrent.CompletableFuture;

@Global
public class TestGlobalFilter implements ServiceActionFilter
{
    @Override
    public CompletableFuture<Boolean> early(FilterContext context)
    {
        CowherdTest.testGlobalFilterTriggered = true;
        CowherdTest.testGlobalFilterCount++;
        return CompletableFuture.completedFuture(true);
    }
}