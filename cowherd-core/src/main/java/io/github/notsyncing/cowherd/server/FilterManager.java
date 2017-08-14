package io.github.notsyncing.cowherd.server;

import io.github.notsyncing.cowherd.annotations.Global;
import io.github.notsyncing.cowherd.annotations.InstantiateType;
import io.github.notsyncing.cowherd.annotations.Route;
import io.github.notsyncing.cowherd.models.FilterInfo;
import io.github.notsyncing.cowherd.models.RouteInfo;
import io.github.notsyncing.cowherd.service.ComponentInstantiateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FilterManager
{
    static List<FilterInfo> globalFilters = new ArrayList<>();
    static Map<RouteInfo, FilterInfo> routedFilters = new ConcurrentHashMap<>();
    static Map<Class<? extends ServiceActionFilter>, FilterInfo> normalFilters = new ConcurrentHashMap<>();

    private static CowherdLogger log = CowherdLogger.getInstance(FilterManager.class);

    public static List<FilterInfo> getGlobalFilters()
    {
        return globalFilters;
    }

    public static Map<RouteInfo, FilterInfo> getRoutedFilters()
    {
        return routedFilters;
    }

    public static Map<Class<? extends ServiceActionFilter>, FilterInfo> getNormalFilters()
    {
        return normalFilters;
    }

    static FilterInfo createFilterInfo(Class<? extends ServiceActionFilter> filterClass)
    {
        FilterInfo info = new FilterInfo();
        info.setFilterClass(filterClass);
        info.setInstantiateType(filterClass.isAnnotationPresent(InstantiateType.class) ?
                filterClass.getAnnotation(InstantiateType.class).value() : ComponentInstantiateType.Singleton);

        if (info.getInstantiateType() == ComponentInstantiateType.Singleton) {
            try {
                info.setFilterInstance(filterClass.newInstance());
            } catch (Exception e) {
                log.e("Failed to create filter " + filterClass, e);
            }
        }
        return info;
    }

    public static void addFilterClass(Class<? extends ServiceActionFilter> filterClass)
    {
        if (filterClass.isAnnotationPresent(Global.class)) {
            addGlobalFilterClass(filterClass);
        } else if (filterClass.isAnnotationPresent(Route.class)) {
            addRoutedFilterClass(filterClass);
        } else {
            addNormalFilterClass(filterClass);
        }
    }

    public static void addGlobalFilterClass(Class<? extends ServiceActionFilter> filterClass)
    {
        if (!filterClass.isAnnotationPresent(Global.class)) {
            return;
        }

        if (globalFilters.stream().anyMatch(f -> f.getFilterClass().equals(filterClass))) {
            return;
        }

        FilterInfo info = createFilterInfo(filterClass);
        globalFilters.add(info);
    }

    public static void addRoutedFilterClass(Class<? extends ServiceActionFilter> filterClass)
    {
        if (!filterClass.isAnnotationPresent(Route.class)) {
            return;
        }

        Route[] routes = filterClass.getAnnotationsByType(Route.class);

        for (Route route : routes) {
            RouteInfo info = new RouteInfo();
            info.setPath(route.value());
            info.setDomain(route.domain());
            info.setFastRoute(route.fastRoute());

            if (routedFilters.keySet().stream().anyMatch(r -> r.equals(info))) {
                return;
            }

            routedFilters.put(info, createFilterInfo(filterClass));
        }
    }

    public static void addNormalFilterClass(Class<? extends ServiceActionFilter> filterClass)
    {
        FilterInfo info = createFilterInfo(filterClass);
        normalFilters.put(filterClass, info);
    }

    public static boolean isFilterClassAdded(Class<? extends ServiceActionFilter> filterClass)
    {
        return (isGlobalFilter(filterClass)) || (isRoutedFilter(filterClass)) || (isNormalFilter(filterClass));
    }

    public static boolean isGlobalFilter(Class<? extends ServiceActionFilter> filterClass)
    {
        return globalFilters.stream().anyMatch(f -> f.getFilterClass().equals(filterClass));
    }

    public static boolean isRoutedFilter(Class<? extends ServiceActionFilter> filterClass)
    {
        return routedFilters.values().stream().anyMatch(f -> f.getFilterClass().equals(filterClass));
    }

    public static boolean isNormalFilter(Class<? extends ServiceActionFilter> filterClass)
    {
        return normalFilters.containsKey(filterClass);
    }
}
