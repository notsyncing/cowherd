package io.github.notsyncing.cowherd.models;

import io.github.notsyncing.cowherd.annotations.Namespace;
import io.github.notsyncing.cowherd.service.CowherdService;
import io.github.notsyncing.cowherd.service.ServiceInstantiateType;

public class CowherdServiceInfo
{
    private Class<? extends CowherdService> serviceClass;
    private CowherdService serviceInstance;
    private ServiceInstantiateType instantiateType = ServiceInstantiateType.SingleInstance;
    private RouteInfo customRoute;
    private String namespace;
    private String name;
    private String fullName;

    public Class<? extends CowherdService> getServiceClass()
    {
        return serviceClass;
    }

    public void setServiceClass(Class<? extends CowherdService> serviceClass)
    {
        this.serviceClass = serviceClass;

        if (serviceClass.isAnnotationPresent(Namespace.class)) {
            this.namespace = serviceClass.getAnnotation(Namespace.class).value();
        }

        this.name = serviceClass.getSimpleName();
        this.fullName = serviceClass.getName();
    }

    public CowherdService getServiceInstance()
    {
        return serviceInstance;
    }

    public void setServiceInstance(CowherdService serviceInstance)
    {
        this.serviceInstance = serviceInstance;

        setServiceClass(serviceInstance.getClass());
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

    public String getNamespace()
    {
        return namespace;
    }

    public String getName()
    {
        return name;
    }

    public String getFullName()
    {
        return fullName;
    }
}
