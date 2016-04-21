package io.github.notsyncing.cowherd.service;

import io.github.notsyncing.cowherd.annotations.Component;
import io.github.notsyncing.cowherd.models.ComponentInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DependencyInjector
{
    private static Map<Class, ComponentInfo> components = new ConcurrentHashMap<>();
    private static Map<Class, Object> singletons = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private static <T> T createInstance(Class<T> type) throws InstantiationException, InvocationTargetException, IllegalAccessException
    {
        ComponentInfo info = components.get(type);
        if (info == null) {
            throw new InstantiationException("Type " + type + " is not registered!");
        }

        if ((info.getCreateType() == ComponentInstantiateType.Singleton)
                && (singletons.containsKey(type))) {
            return (T)singletons.get(type);
        }

        Constructor[] constructors = info.getType().getConstructors();
        if (constructors.length <= 0) {
            throw new InstantiationException("Type " + info.getType()
                    + " has no public constructor!");
        }

        Constructor constructor = constructors[0];
        Class[] params = constructor.getParameterTypes();
        T object;

        if (params.length <= 0) {
            object = (T)constructor.newInstance();
        } else {
            List<Object> paramList = new ArrayList<>();

            for (Class p : params) {
                Object rp = createInstance(p);
                if (rp == null) {
                    throw new InstantiationException("Failed to create type "
                            + p + " for type " + type);
                }

                paramList.add(rp);
            }

            object = (T)constructor.newInstance(paramList.toArray());
        }

        if (info.getCreateType() == ComponentInstantiateType.Singleton) {
            singletons.put(type, object);
        }

        System.out.println("Created object " + object);
        return object;
    }

    public static void registerComponent(Class interfaceType, Class objectType, ComponentInstantiateType createType,
                                         boolean createNow)
    {
        if (components.containsKey(interfaceType)) {
            return;
        }

        ComponentInfo info = new ComponentInfo();
        info.setCreateType(createType);
        info.setType(objectType);
        info.setInterfaceType(interfaceType);

        components.put(interfaceType, info);

        System.out.println("DependencyInjector: Registered component " + objectType);

        if ((createNow) && (createType == ComponentInstantiateType.Singleton)) {
            try {
                makeObject(interfaceType);
            } catch (Exception e) {
                System.out.println("DependencyInjector: Failed to make object " + interfaceType + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void registerComponent(Class type, ComponentInstantiateType createType, boolean createNow)
    {
        registerComponent(type, type, createType, createNow);
    }

    public static void registerComponent(Class type, Object o, boolean createNow)
    {
        registerComponent(type, ComponentInstantiateType.Singleton, createNow);
        singletons.put(type, o);
    }

    public static void registerComponent(Object o, boolean createNow)
    {
        registerComponent(o.getClass(), ComponentInstantiateType.Singleton, createNow);
        singletons.put(o.getClass(), o);
    }

    public static void registerComponent(Class c)
    {
        if (!c.isAnnotationPresent(Component.class)) {
            return;
        }

        Component componentInfo = (Component)c.getAnnotation(Component.class);
        registerComponent(c, componentInfo.value(), componentInfo.createAtRegister());
    }

    public static <T> T getComponent(Class<T> type) throws InstantiationException, InvocationTargetException, IllegalAccessException
    {
        return createInstance(type);
    }

    public static Object getComponent(String className) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        Class c = DependencyInjector.class.getClassLoader().loadClass(className);
        return getComponent(c);
    }

    public static boolean hasComponent(Class<?> type)
    {
        return components.containsKey(type);
    }

    public static <T> T makeObject(Class<T> type) throws IllegalAccessException, InvocationTargetException, InstantiationException
    {
        if (!hasComponent(type)) {
            registerComponent(type, ComponentInstantiateType.AlwaysNew, false);
        }

        return createInstance(type);
    }
}
