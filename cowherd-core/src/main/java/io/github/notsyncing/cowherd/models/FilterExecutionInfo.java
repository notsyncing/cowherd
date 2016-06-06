package io.github.notsyncing.cowherd.models;

import java.util.HashMap;
import java.util.Map;

public class FilterExecutionInfo
{
    private FilterInfo filter;
    private Map<String, String> parameters = new HashMap<>();
    private FilterContext context;

    public FilterExecutionInfo()
    {
    }

    public FilterExecutionInfo(FilterInfo filter)
    {
        this.filter = filter;
    }

    public FilterInfo getFilter()
    {
        return filter;
    }

    public void setFilter(FilterInfo filter)
    {
        this.filter = filter;
    }

    public Map<String, String> getParameters()
    {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters)
    {
        this.parameters = parameters;
    }

    public void addParameter(String name, String value)
    {
        parameters.put(name, value);
    }

    public FilterContext getContext()
    {
        return context;
    }

    public void setContext(FilterContext context)
    {
        this.context = context;
    }
}
