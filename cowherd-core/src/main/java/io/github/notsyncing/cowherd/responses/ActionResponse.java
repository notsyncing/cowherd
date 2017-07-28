package io.github.notsyncing.cowherd.responses;

import io.github.notsyncing.cowherd.models.ActionContext;

import java.io.IOException;

public interface ActionResponse
{
    void writeToResponse(ActionContext context) throws IOException;
}
