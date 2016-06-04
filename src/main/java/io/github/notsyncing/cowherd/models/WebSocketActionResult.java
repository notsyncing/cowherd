package io.github.notsyncing.cowherd.models;

import java.lang.reflect.Method;

public class WebSocketActionResult extends ActionResult
{
    public WebSocketActionResult()
    {
    }

    public WebSocketActionResult(Method actionMethod, Object result)
    {
        super(actionMethod, result);
    }
}
