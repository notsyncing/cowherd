package io.github.notsyncing.cowherd.commons;

public class AlternativeCookieHeaderConfig
{
    private String onlyOn;
    private String setCookie;
    private String cookie;

    public String getOnlyOn()
    {
        return onlyOn;
    }

    public void setOnlyOn(String onlyOn)
    {
        this.onlyOn = onlyOn;
    }

    public String getSetCookie()
    {
        return setCookie;
    }

    public void setSetCookie(String setCookie)
    {
        this.setCookie = setCookie;
    }

    public String getCookie()
    {
        return cookie;
    }

    public void setCookie(String cookie)
    {
        this.cookie = cookie;
    }
}
