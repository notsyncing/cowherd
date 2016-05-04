package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.models.ActionResult;
import io.github.notsyncing.cowherd.models.FilterContext;
import io.github.notsyncing.cowherd.server.ServiceActionFilter;
import io.github.notsyncing.cowherd.tests.CowherdTest;

import java.util.concurrent.CompletableFuture;

public class TestFilter implements ServiceActionFilter
{
    @Override
    public CompletableFuture<Boolean> early(FilterContext context)
    {
        CowherdTest.testFilterTriggered = true;
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> before(FilterContext context)
    {
        CowherdTest.testFilterRequestParameters = context.getRequestParameters();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<ActionResult> after(FilterContext context)
    {
        CowherdTest.testFilterRequestResult = context.getResult();
        return CompletableFuture.completedFuture(context.getResult());
    }
}
