package io.github.notsyncing.cowherd.models;

import io.github.notsyncing.cowherd.utils.StringUtils;

import java.util.regex.Pattern;

public class RouteInfo
{
    private String domain;
    private String path;
    private Pattern domainPattern;
    private Pattern pathPattern;

    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public Pattern getDomainPattern()
    {
        if (domainPattern == null) {
            if (StringUtils.isEmpty(domain)) {
                return null;
            }

            domainPattern = Pattern.compile(domain);
        }

        return domainPattern;
    }

    public Pattern getPathPattern()
    {
        if (pathPattern == null) {
            if (StringUtils.isEmpty(path)) {
                return null;
            }

            pathPattern = Pattern.compile(path);
        }

        return pathPattern;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof RouteInfo)) return false;

        RouteInfo routeInfo = (RouteInfo) o;

        return getDomain().equals(routeInfo.getDomain()) && getPath().equals(routeInfo.getPath());

    }

    @Override
    public int hashCode()
    {
        int result = getDomain().hashCode();
        result = 31 * result + getPath().hashCode();
        return result;
    }
}
