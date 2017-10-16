package io.github.notsyncing.cowherd.models;

import io.vertx.core.http.HttpServerRequest;

public class RequestDelegationInfo {
    private HttpServerRequest request;
    private boolean delegated;
    private Object tag;

    public HttpServerRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServerRequest request) {
        this.request = request;
    }

    public boolean isDelegated() {
        return delegated;
    }

    public void setDelegated(boolean delegated) {
        this.delegated = delegated;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }
}
