package io.github.notsyncing.cowherd.responses;

import io.github.notsyncing.cowherd.models.ActionContext;

import java.io.IOException;

public class RedirectResponse implements ActionResponse
{
    private String url;

    public RedirectResponse(String url)
    {
        this.url = url;
    }

    @Override
    public void writeToResponse(ActionContext context) throws IOException
    {
        context.getRequest().response().setStatusCode(302);
        context.getRequest().response().putHeader("Location", url);
        context.getRequest().response().end();
    }
}
