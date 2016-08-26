package io.github.notsyncing.cowherd.service;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.notsyncing.cowherd.Cowherd;
import io.github.notsyncing.cowherd.annotations.Component;
import io.github.notsyncing.cowherd.models.ComponentInfo;
import io.github.notsyncing.cowherd.server.CowherdLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 依赖注入器
 * 用于自动构建对象并填充其所依赖的对象
 */
public class CowherdDependencyInjector implements DependencyInjector {
    private Map<Class, ComponentInfo> components = new ConcurrentHashMap<>();
    private Map<Class, Object> singletons = new ConcurrentHashMap<>();

    private static FastClasspathScanner scanner;

    private CowherdLogger log = CowherdLogger.getInstance(CowherdDependencyInjector.class);

    public CowherdDependencyInjector(boolean noScan)
    {
        if (!noScan) {
            scanner.matchClassesWithAnnotation(Component.class, this::registerComponent)
                    .scan();
        }
    }

    public CowherdDependencyInjector()
    {
        this(true);
    }

    public static FastClasspathScanner getScanner()
    {
        return scanner;
    }

    public static void setScanner(FastClasspathScanner scanner)
    {
        CowherdDependencyInjector.scanner = scanner;
    }

    /**
     * 清除依赖注入器的对象缓存和已注册类列表
     */
    @Override
    public void clear()
    {
        components.clear();
        singletons.clear();
    }

    @Override
    public void init()
    {
        classScanCompleted();
    }

    @SuppressWarnings("unchecked")
    private <T> T createInstance(Class<T> type) throws InstantiationException, InvocationTargetException, IllegalAccessException
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

        log.d("Created object " + object);
        return object;
    }

    /**
     * 向依赖注入器注册一个类
     * @param interfaceType 该类的接口类型
     * @param objectType 该类的类型
     * @param createType 实例化方式
     * @param createEarly 是否在依赖注入器扫描完 Classpath 之后立即实例化，仅对实例化方式为 {@link ComponentInstantiateType#Singleton} 的类有效
     */
    @Override
    public void registerComponent(Class interfaceType, Class objectType, ComponentInstantiateType createType,
                                  boolean createEarly)
    {
        if (components.containsKey(interfaceType)) {
            return;
        }

        ComponentInfo info = new ComponentInfo();
        info.setCreateType(createType);
        info.setType(objectType);
        info.setInterfaceType(interfaceType);
        info.setCreateEarly(createEarly);

        components.put(interfaceType, info);

        log.d("Registered component " + objectType);
    }

    /**
     * 向依赖注入器注册一个类
     * @param type 该类的类型
     * @param createType 实例化方式
     * @param createEarly 是否在依赖注入器扫描完 Classpath 之后立即实例化，仅对实例化方式为 {@link ComponentInstantiateType#Singleton} 的类有效
     */
    @Override
    public void registerComponent(Class type, ComponentInstantiateType createType, boolean createEarly)
    {
        registerComponent(type, type, createType, createEarly);
    }

    /**
     * 以单实例方式，向依赖注入器注册一个对象
     * @param type 要注册的类型
     * @param o 要注册的对象
     */
    @Override
    public void registerComponent(Class type, Object o)
    {
        registerComponent(type, ComponentInstantiateType.Singleton, false);
        singletons.put(type, o);
    }

    /**
     * 以单实例方式，向依赖注入器注册一个对象
     * @param o 要注册的对象
     */
    @Override
    public void registerComponent(Object o)
    {
        registerComponent(o.getClass(), ComponentInstantiateType.Singleton, false);
        singletons.put(o.getClass(), o);
    }

    /**
     * 向依赖注入器注册一个具有依赖注入注解的类
     * @param c 要注册的类
     */
    @Override
    public void registerComponent(Class c)
    {
        if (!c.isAnnotationPresent(Component.class)) {
            return;
        }

        Component componentInfo = (Component)c.getAnnotation(Component.class);
        registerComponent(c, componentInfo.value(), componentInfo.createEarly());
    }

    private void classScanCompleted()
    {
        for (ComponentInfo info : components.values()) {
            if ((info.isCreateEarly()) && (info.getCreateType() == ComponentInstantiateType.Singleton)) {
                try {
                    makeObject(info.getInterfaceType());
                } catch (Exception e) {
                    log.e("Failed to make object " + info.getInterfaceType() + ": " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 获取一个类的实例
     * @param type 要获取的类的类型
     * @param <T> 要获取的类
     * @return 该类的实例
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @Override
    public <T> T getComponent(Class<T> type) throws InstantiationException, InvocationTargetException, IllegalAccessException
    {
        return createInstance(type);
    }

    /**
     * 根据名字获取一个类的实例
     * @param className 要获取的类的完整名称
     * @return 该类的实例
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    @Override
    public Object getComponent(String className) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        Class c = CowherdDependencyInjector.class.getClassLoader().loadClass(className);
        return getComponent(c);
    }

    /**
     * 检查是否向依赖注入器注册了一个类
     * @param type 要检查的类型
     * @return 该类型是否已注册
     */
    @Override
    public boolean hasComponent(Class<?> type)
    {
        return components.containsKey(type);
    }

    /**
     * 构造一个对象，若该类型未在依赖注入器中注册，则以 {@link ComponentInstantiateType#AlwaysNew} 实例化方式注册之
     * @param type 要构造的对象类型
     * @param <T> 要构造的对象类型
     * @return 构造出的对象
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    @Override
    public <T> T makeObject(Class<T> type) throws IllegalAccessException, InvocationTargetException, InstantiationException
    {
        if (!hasComponent(type)) {
            registerComponent(type, ComponentInstantiateType.AlwaysNew, false);
        }

        return createInstance(type);
    }
}
