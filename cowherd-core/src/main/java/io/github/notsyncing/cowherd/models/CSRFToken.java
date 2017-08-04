package io.github.notsyncing.cowherd.models;

import java.util.Date;

public class CSRFToken {
    private String token;
    private Date expireTime;

    public CSRFToken(String token, Date expireTime) {
        this.token = token;
        this.expireTime = expireTime;
    }

    public CSRFToken(String token) {
        this(token, new Date());
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CSRFToken) {
            return token.equals(((CSRFToken)obj).token);
        }

        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }
}
