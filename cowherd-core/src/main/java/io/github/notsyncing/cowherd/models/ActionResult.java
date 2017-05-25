package io.github.notsyncing.cowherd.models;

public class ActionResult
{
    private ActionContext context;
    private Object result;

    public ActionResult()
    {
        context = new ActionContext();
    }

    public ActionResult(ActionContext context, Object result)
    {
        this.context = context;
        this.result = result;
    }

    public ActionContext getContext()
    {
        return context;
    }

    public void setContext(ActionContext context)
    {
        this.context = context;
    }

    public Object getResult()
    {
        return result;
    }

    public void setResult(Object result)
    {
        this.result = result;
    }
}
