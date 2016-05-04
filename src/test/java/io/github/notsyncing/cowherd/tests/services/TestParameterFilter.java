package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.models.FilterContext;
import io.github.notsyncing.cowherd.server.ServiceActionFilter;
import io.github.notsyncing.cowherd.tests.CowherdTest;

import java.util.concurrent.CompletableFuture;

public class TestParameterFilter implements ServiceActionFilter
{
    @Override
    public CompletableFuture<Boolean> filter(FilterContext context)
    {
        CowherdTest.testFilterParameters = context.getFilterParameters();
        return CompletableFuture.completedFuture(true);
    }
}
