package io.github.notsyncing.cowherd.models;

import java.lang.reflect.Method;

public class ActionResult
{
    private Method actionMethod;
    private Object result;

    public ActionResult()
    {
    }

    public ActionResult(Method actionMethod, Object result)
    {
        this.actionMethod = actionMethod;
        this.result = result;
    }

    public Method getActionMethod()
    {
        return actionMethod;
    }

    public void setActionMethod(Method actionMethod)
    {
        this.actionMethod = actionMethod;
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
