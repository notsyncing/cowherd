package io.github.notsyncing.cowherd.stress;

import io.github.notsyncing.cowherd.annotations.Exported;
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpGet;
import io.github.notsyncing.cowherd.service.CowherdService;

public class TestService extends CowherdService
{
    @Exported
    @HttpGet
    public String simpleRequest()
    {
        return "Hello, world!";
    }
}
