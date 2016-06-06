package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.annotations.Component;
import io.github.notsyncing.cowherd.authentication.ActionAuthenticator;
import io.github.notsyncing.cowherd.authentication.annotations.ServiceActionAuthenticator;
import io.github.notsyncing.cowherd.models.FilterContext;
import io.github.notsyncing.cowherd.tests.CowherdTest;

import java.util.concurrent.CompletableFuture;

@Component
public class TestAuthenticator implements ActionAuthenticator<TestAuth>
{
    @Override
    public CompletableFuture<Boolean> authenticate(TestAuth authAnnotation, FilterContext context)
    {
        CowherdTest.testAuthenticatorTriggered = true;
        CowherdTest.testAuthenticatorTriggerCount++;

        if (context.getRequestParameters().containsKey("nopass")) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.completedFuture(true);
    }
}
