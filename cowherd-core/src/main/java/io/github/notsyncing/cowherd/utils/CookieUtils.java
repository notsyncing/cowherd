package io.github.notsyncing.cowherd.utils;

import java.net.HttpCookie;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CookieUtils
{
    static final DateFormat cookiesDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z");

    public static String cookieToString(HttpCookie cookie)
    {
        StringBuilder b = new StringBuilder();
        b.append(cookie.getName()).append("=").append(cookie.getValue());

        if (cookie.getPath() != null) {
            b.append("; path=").append(cookie.getPath());
        }

        if (cookie.getDomain() != null) {
            b.append("; domain=").append(cookie.getDomain());
        }

        if (cookie.getPortlist() != null) {
            b.append("; port=").append(cookie.getPortlist());
        }

        if (cookie.getMaxAge() != -1) {
            b.append("; max-age=").append(cookie.getMaxAge());

            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.add(Calendar.SECOND, (int)cookie.getMaxAge());
            b.append("; expires=").append(cookiesDateFormat.format(c.getTime()));
        }

        if (cookie.getSecure()) {
            b.append("; secure");
        }

        if (cookie.isHttpOnly()) {
            b.append("; httponly");
        }

        return b.toString();
    }

    public static List<HttpCookie> parseServerCookies(String cookies)
    {
        String[] cookiePairs = cookies.split(";");

        if (cookiePairs.length <= 0) {
            return null;
        }

        List<HttpCookie> list = new ArrayList<>();

        for (String cp : cookiePairs) {
            String[] nv = cp.split("=");

            if (nv.length <= 1) {
                continue;
            }

            HttpCookie cookie = new HttpCookie(nv[0].trim(), nv[1].trim());
            list.add(cookie);
        }

        return list;
    }
}
