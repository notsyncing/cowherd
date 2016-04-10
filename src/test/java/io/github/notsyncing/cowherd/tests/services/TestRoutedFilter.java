package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.annotations.Route;
import io.github.notsyncing.cowherd.server.ServiceActionFilter;
import io.github.notsyncing.cowherd.tests.CowherdTest;

import java.util.concurrent.CompletableFuture;

@Route("/TestService/")
public class TestRoutedFilter implements ServiceActionFilter
{
    @Override
    public CompletableFuture<Boolean> filter()
    {
        CowherdTest.testRoutedFilterTriggered = true;
        return CompletableFuture.completedFuture(true);
    }
}
