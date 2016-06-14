package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.Cowherd;
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
        CowherdTest.testGlobalFilterEarlyTriggerCount++;
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> before(FilterContext context)
    {
        CowherdTest.testGlobalFilterBeforeTriggerCount++;

        if (context.getRequestParameters().stream().anyMatch(p -> p.getKey().equals("nopassGlobal"))) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.completedFuture(true);
    }
}