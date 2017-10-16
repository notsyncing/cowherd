package io.github.notsyncing.cowherd.models;

public class RequestDoneInfo {
    private long time;
    private RequestDelegationInfo delegationInfo;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public RequestDelegationInfo getDelegationInfo() {
        return delegationInfo;
    }

    public void setDelegationInfo(RequestDelegationInfo delegationInfo) {
        this.delegationInfo = delegationInfo;
    }
}
