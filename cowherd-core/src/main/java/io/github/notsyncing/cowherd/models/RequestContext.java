package io.github.notsyncing.cowherd.models;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.util.ArrayList;
import java.util.List;

public class RequestContext
{
    private HttpMethod method;
    private String path;
    private MultiMap headers;
    private List<Pair<String, String>> parameters = new ArrayList<>();
    private List<UploadFileInfo> uploads = new ArrayList<>();
    private HttpServerRequest request;
    private HttpServerResponse response;

    public HttpMethod getMethod()
    {
        return method;
    }

    public void setMethod(HttpMethod method)
    {
        this.method = method;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public MultiMap getHeaders()
    {
        return headers;
    }

    public void setHeaders(MultiMap headers)
    {
        this.headers = headers;
    }

    public List<Pair<String, String>> getParameters()
    {
        return parameters;
    }

    public void setParameters(List<Pair<String, String>> parameters)
    {
        this.parameters = parameters;
    }

    public List<UploadFileInfo> getUploads()
    {
        return uploads;
    }

    public void setUploads(List<UploadFileInfo> uploads)
    {
        this.uploads = uploads;
    }

    public HttpServerRequest getRequest()
    {
        return request;
    }

    public void setRequest(HttpServerRequest request)
    {
        this.request = request;
    }

    public HttpServerResponse getResponse()
    {
        return response;
    }

    public void setResponse(HttpServerResponse response)
    {
        this.response = response;
    }
}
