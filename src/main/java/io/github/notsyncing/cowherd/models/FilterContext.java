package io.github.notsyncing.cowherd.models;

import java.util.Map;

public class FilterContext
{
    private Map<String, String> filterParameters;

    public Map<String, String> getFilterParameters()
    {
        return filterParameters;
    }

    public void setFilterParameters(Map<String, String> filterParameters)
    {
        this.filterParameters = filterParameters;
    }
}
