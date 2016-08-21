package io.github.notsyncing.cowherd.service;

import io.github.notsyncing.cowherd.Cowherd;
import io.github.notsyncing.cowherd.annotations.InstantiateType;
import io.github.notsyncing.cowherd.exceptions.InvalidServiceActionException;
import io.github.notsyncing.cowherd.models.CowherdServiceInfo;
import io.github.notsyncing.cowherd.models.RouteInfo;
import io.github.notsyncing.cowherd.server.CowherdLogger;
import io.github.notsyncing.cowherd.server.RouteManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceManager
{
    private static Map<String, CowherdServiceInfo> services = new ConcurrentHashMap<>();
    private static Map<String, CowherdService> serviceInstances = new ConcurrentHashMap<>();

    private static CowherdLogger log = CowherdLogger.getInstance(ServiceManager.class);

    private static void processServiceAnnotations(CowherdServiceInfo info)
    {
        Class<? extends CowherdService> serviceClass = info.getServiceClass();

        if (serviceClass.isAnnotationPresent(InstantiateType.class)) {
            info.setInstantiateType(serviceClass.getAnnotation(InstantiateType.class).value());
        }
    }

    private static void addServiceInfo(String name, CowherdServiceInfo info, RouteInfo customRoute) throws InvalidServiceActionException
    {
        Cowherd.dependencyInjector.registerComponent(info.getServiceClass(), info.getInstantiateType(), false);

        processServiceAnnotations(info);

        info.setCustomRoute(customRoute);

        services.put(name, info);

        RouteManager.addRoutesInService(info.getServiceClass(), info);
    }

    public static void addServiceInstance(CowherdService service, RouteInfo customRoute) throws InvalidServiceActionException
    {
        String name = service.getClass().getName();

        if (!services.containsKey(name)) {
            CowherdServiceInfo info = new CowherdServiceInfo();
            info.setServiceClass(service.getClass());
            info.setServiceInstance(service);

            addServiceInfo(name, info, customRoute);
        } else {
            CowherdServiceInfo info = services.get(name);
            info.setServiceInstance(service);
        }
    }

    public static void addServiceClass(Class<? extends CowherdService> serviceClass, RouteInfo customRoute)
    {
        String name = serviceClass.getName();

        try {
            if (!services.containsKey(name)) {
                CowherdServiceInfo info = new CowherdServiceInfo();
                info.setServiceClass(serviceClass);

                addServiceInfo(name, info, customRoute);

                log.d("Added service " + serviceClass);
            }
        } catch (Exception e) {
            log.e("Failed to add service class " + serviceClass, e);
        }
    }

    public static void addServiceClass(Class<? extends CowherdService> serviceClass)
    {
        addServiceClass(serviceClass, null);
    }

    public static CowherdService getServiceInstance(Class<? extends CowherdService> serviceClass) throws IllegalAccessException, InstantiationException, InvocationTargetException
    {
        CowherdServiceInfo info = services.get(serviceClass.getName());

        if (info == null) {
            return null;
        }

        if (info.getInstantiateType() == ComponentInstantiateType.AlwaysNew) {
            return Cowherd.dependencyInjector.makeObject(serviceClass);
        } else if (info.getInstantiateType() == ComponentInstantiateType.Singleton) {
            if (!serviceInstances.containsKey(serviceClass.getName())) {
                CowherdService s = Cowherd.dependencyInjector.makeObject(serviceClass);
                serviceInstances.put(serviceClass.getName(), s);
                return s;
            } else {
                return serviceInstances.get(serviceClass.getName());
            }
        } else {
            throw new InstantiationException("Unsupported instantiate type " + info.getInstantiateType() +
                    " on service " + serviceClass);
        }
    }

    public static Method getServiceAction(String serviceName, String actionName)
    {
        CowherdServiceInfo info = services.entrySet().stream()
                .filter(e -> e.getKey().endsWith(serviceName.contains(".") ? serviceName : ("." + serviceName)))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        for (Method m : info.getServiceClass().getMethods()) {
            if (m.getName().equals(actionName)) {
                return m;
            }
        }

        return null;
    }

    public static boolean isServiceClassAdded(Class<? extends CowherdService> serviceClass)
    {
        return services.values().stream().anyMatch(i -> i.getServiceClass().equals(serviceClass));
    }

    public static Collection<CowherdServiceInfo> getServices()
    {
        return services.values();
    }

    public static void instantiateSingletonServices() throws IllegalAccessException, InvocationTargetException, InstantiationException
    {
        for (CowherdServiceInfo info : services.values()) {
            if (info.getInstantiateType() == ComponentInstantiateType.Singleton) {
                if (getServiceInstance(info.getServiceClass()) == null) {
                    log.e("Failed to instantiate service: " + info.getServiceClass());
                }
            }
        }
    }

    public static void clear()
    {
        services.clear();
        serviceInstances.clear();
    }
}
