package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.annotations.Route;
import io.github.notsyncing.cowherd.models.FilterContext;
import io.github.notsyncing.cowherd.server.ServiceActionFilter;
import io.github.notsyncing.cowherd.tests.CowherdTest;

import java.util.concurrent.CompletableFuture;

@Route("/TestService/")
public class TestRoutedFilter implements ServiceActionFilter
{
    @Override
    public CompletableFuture<Boolean> early(FilterContext context)
    {
        CowherdTest.testRoutedFilterTriggered = true;
        CowherdTest.testRoutedFilterCount++;
        return CompletableFuture.completedFuture(true);
    }
}
