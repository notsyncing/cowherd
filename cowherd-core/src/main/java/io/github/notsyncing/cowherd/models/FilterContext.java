package io.github.notsyncing.cowherd.models;

import io.vertx.core.http.HttpServerRequest;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;

public class FilterContext
{
    private Map<String, String> filterParameters;
    private HttpServerRequest request;
    private List<UploadFileInfo> requestUploads;
    private List<Pair<String, String>> requestParameters;
    private List<HttpCookie> requestCookies;
    private ActionResult result;

    public Map<String, String> getFilterParameters()
    {
        return filterParameters;
    }

    public void setFilterParameters(Map<String, String> filterParameters)
    {
        this.filterParameters = filterParameters;
    }

    public HttpServerRequest getRequest()
    {
        return request;
    }

    public void setRequest(HttpServerRequest request)
    {
        this.request = request;
    }

    public List<UploadFileInfo> getRequestUploads()
    {
        return requestUploads;
    }

    public void setRequestUploads(List<UploadFileInfo> requestUploads)
    {
        this.requestUploads = requestUploads;
    }

    public List<Pair<String, String>> getRequestParameters()
    {
        return requestParameters;
    }

    public void setRequestParameters(List<Pair<String, String>> requestParameters)
    {
        this.requestParameters = requestParameters;
    }

    public List<HttpCookie> getRequestCookies()
    {
        return requestCookies;
    }

    public void setRequestCookies(List<HttpCookie> requestCookies)
    {
        this.requestCookies = requestCookies;
    }

    public ActionResult getResult()
    {
        return result;
    }

    public void setResult(ActionResult result)
    {
        this.result = result;
    }
}
