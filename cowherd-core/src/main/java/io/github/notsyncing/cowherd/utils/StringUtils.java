package io.github.notsyncing.cowherd.utils;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class StringUtils
{

    static {
        CookieUtils.cookiesDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static String appendUrl(String source, String append)
    {
        return source.endsWith("/") ? source + append : source + "/" + append;
    }

    public static boolean isEmpty(String s)
    {
        return (s == null) || (s.isEmpty());
    }

    public static String exceptionStackToString(Throwable ex)
    {
        try (StringWriter writer = new StringWriter(); PrintWriter w = new PrintWriter(writer)) {
            ex.printStackTrace(w);
            return writer.toString();
        } catch (Exception ex2) {
            ex2.printStackTrace();
        }

        return null;
    }

    public static String streamToString(InputStream s) throws IOException
    {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;

        while ((length = s.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        return result.toString("UTF-8");
    }

    public static Date parseHttpDateString(String s) throws ParseException
    {
        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).parse(s);
    }

    public static String dateToHttpDateString(Date d)
    {
        DateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f.format(d);
    }

    public static boolean isInteger(String str)
    {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
