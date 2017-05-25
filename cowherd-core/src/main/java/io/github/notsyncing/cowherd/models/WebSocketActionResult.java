package io.github.notsyncing.cowherd.models;

public class WebSocketActionResult extends ActionResult
{
    public WebSocketActionResult()
    {
    }

    public WebSocketActionResult(ActionContext context, Object result)
    {
        super(context, result);
    }
}
