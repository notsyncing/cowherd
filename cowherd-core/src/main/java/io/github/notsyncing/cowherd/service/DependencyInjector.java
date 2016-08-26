package io.github.notsyncing.cowherd.service;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

import java.lang.reflect.InvocationTargetException;

public interface DependencyInjector {
    void clear();

    void init();

    void registerComponent(Class interfaceType, Class objectType, ComponentInstantiateType createType,
                           boolean createEarly);

    void registerComponent(Class type, ComponentInstantiateType createType, boolean createEarly);

    void registerComponent(Class type, Object o);

    void registerComponent(Object o);

    void registerComponent(Class c);

    <T> T getComponent(Class<T> type) throws InstantiationException, InvocationTargetException, IllegalAccessException;

    Object getComponent(String className) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException;

    boolean hasComponent(Class<?> type);

    <T> T makeObject(Class<T> type) throws IllegalAccessException, InvocationTargetException, InstantiationException;
}
