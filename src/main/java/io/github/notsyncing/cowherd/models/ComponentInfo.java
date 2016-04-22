package io.github.notsyncing.cowherd.models;

import io.github.notsyncing.cowherd.service.ComponentInstantiateType;

public class ComponentInfo
{
    private Class<?> interfaceType;
    private Class<?> type;
    private ComponentInstantiateType createType;
    private boolean createEarly;

    public Class<?> getInterfaceType()
    {
        return interfaceType;
    }

    public void setInterfaceType(Class<?> interfaceType)
    {
        this.interfaceType = interfaceType;
    }

    public Class<?> getType()
    {
        return type;
    }

    public void setType(Class<?> type)
    {
        this.type = type;
    }

    public ComponentInstantiateType getCreateType()
    {
        return createType;
    }

    public void setCreateType(ComponentInstantiateType createType)
    {
        this.createType = createType;
    }

    public boolean isCreateEarly()
    {
        return createEarly;
    }

    public void setCreateEarly(boolean createEarly)
    {
        this.createEarly = createEarly;
    }
}
