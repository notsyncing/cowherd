package io.github.notsyncing.cowherd.models;

import java.util.HashMap;
import java.util.Map;

public class FilterExecutionInfo
{
    private FilterInfo filter;
    private Map<String, String> parameters = new HashMap<>();

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
}
