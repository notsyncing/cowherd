package io.github.notsyncing.cowherd.models;

import io.github.notsyncing.cowherd.annotations.Namespace;
import io.github.notsyncing.cowherd.service.ComponentInstantiateType;
import io.github.notsyncing.cowherd.service.CowherdService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class CowherdServiceInfo
{
    private Class<? extends CowherdService> serviceClass;
    private CowherdService serviceInstance;
    private ComponentInstantiateType instantiateType = ComponentInstantiateType.Singleton;
    private RouteInfo customRoute;
    private String namespace;
    private String name;
    private String fullName;
    private ConcurrentHashMap<String, ActionMethodInfo> methodMap = new ConcurrentHashMap<>();

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

        Stream.of(serviceClass.getMethods())
                .forEach(m -> methodMap.put(m.getName(), new ActionMethodInfo(m)));
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

    public ComponentInstantiateType getInstantiateType()
    {
        return instantiateType;
    }

    public void setInstantiateType(ComponentInstantiateType instantiateType)
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

    public ConcurrentHashMap<String, ActionMethodInfo> getMethodMap() {
        return methodMap;
    }
}
