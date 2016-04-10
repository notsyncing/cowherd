package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.annotations.Global;
import io.github.notsyncing.cowherd.server.ServiceActionFilter;
import io.github.notsyncing.cowherd.tests.CowherdTest;

import java.util.concurrent.CompletableFuture;

@Global
public class TestGlobalFilter implements ServiceActionFilter
{
    @Override
    public CompletableFuture<Boolean> filter()
    {
        CowherdTest.testGlobalFilterTriggered = true;
        return CompletableFuture.completedFuture(true);
    }
}