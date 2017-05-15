package io.github.notsyncing.cowherd.utils;

import io.github.notsyncing.cowherd.models.Pair;

import java.io.*;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

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
        if ((length == 0) || (length > 8)) {
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

    public static List<Pair<String, String>> parseQueryString(String qs)
    {
        List<Pair<String, String>> l = new ArrayList<>();
        String[] pairs = qs.split("&");
        Pattern skipPattern = Pattern.compile(".*?[<>!].*?");

        try {
            for (String p : pairs) {
                int i = p.indexOf("=");

                if (i < 0) {
                    continue;
                }

                String key = URLDecoder.decode(p.substring(0, i), "utf-8");
                String value = URLDecoder.decode(p.substring(i + 1), "utf-8");

                if (skipPattern.matcher(key).find()) {
                    continue;
                }

                l.add(new Pair<>(key, value));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return l;
    }

    public static String stripSameCharAtStringHeader(String s, char ch)
    {
        if (s.length() > 1) {
            int start = 0;

            for (int i = 1; i < s.length(); i++) {
                if (s.charAt(i) != ch) {
                    start = i - 1;
                    break;
                }
            }

            s = s.substring(start);
        }

        return s;
    }
}
