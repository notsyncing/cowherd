package io.github.notsyncing.cowherd.models;

import io.github.notsyncing.cowherd.server.ServiceActionFilter;
import io.github.notsyncing.cowherd.service.ServiceInstantiateType;

public class FilterInfo
{
    private Class<? extends ServiceActionFilter> filterClass;
    private ServiceActionFilter filterInstance;
    private ServiceInstantiateType instantiateType = ServiceInstantiateType.SingleInstance;
    private RouteInfo customRoute;

    public Class<? extends ServiceActionFilter> getFilterClass()
    {
        return filterClass;
    }

    public void setFilterClass(Class<? extends ServiceActionFilter> filterClass)
    {
        this.filterClass = filterClass;
    }

    public ServiceActionFilter getFilterInstance()
    {
        return filterInstance;
    }

    public void setFilterInstance(ServiceActionFilter filterInstance)
    {
        this.filterInstance = filterInstance;
    }

    public ServiceInstantiateType getInstantiateType()
    {
        return instantiateType;
    }

    public void setInstantiateType(ServiceInstantiateType instantiateType)
    {
        this.instantiateType = instantiateType;
    }

    public RouteInfo getCustomRoute()
    {
        return customRoute;
    }

    public void setCustomRoute(RouteInfo customRoute)
    {
        this.customRoute = customRoute;
    }
}
