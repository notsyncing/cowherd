package io.github.notsyncing.cowherd.utils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.List;

public class TypeUtils
{
    public static Boolean stringToBoolean(String s)
    {
        return ((s != null) && (!s.isEmpty()) && ((s.toLowerCase().equals("true")) || (s.equals("1"))));
    }

    public static Integer stringToInt(String s)
    {
        return Integer.parseInt(s);
    }

    public static Byte stringToByte(String s)
    {
        return Byte.parseByte(s);
    }

    public static Long stringToLong(String s)
    {
        return Long.parseLong(s);
    }

    public static Short stringToShort(String s)
    {
        return Short.parseShort(s);
    }

    public static Float stringToFloat(String s)
    {
        return Float.parseFloat(s);
    }

    public static Double stringToDouble(String s)
    {
        return Double.parseDouble(s);
    }

    @SuppressWarnings("unchecked")
    public static <T> T stringToType(Class<T> c, String s)
    {
        if (c == String.class) {
            return (T)s;
        } else if ((c == boolean.class) || (c == Boolean.class)) {
            return (T)stringToBoolean(s);
        } else if ((c == int.class) || (c == Integer.class)) {
            return (T)stringToInt(s);
        } else if ((c == byte.class) || (c == Byte.class)) {
            return (T)stringToByte(s);
        } else if ((c == char.class) || (c == Character.class)) {
            return (T)(new Character(s.charAt(0)));
        } else if ((c == long.class) || (c == Long.class)) {
            return (T)stringToLong(s);
        } else if ((c == short.class) || (c == Short.class)) {
            return (T)stringToShort(s);
        } else if ((c == float.class) || (c == Float.class)) {
            return (T)stringToFloat(s);
        } else if ((c == double.class) || (c == Double.class)) {
            return (T)stringToDouble(s);
        } else if (c == BigDecimal.class) {
            return (T)new BigDecimal(s);
        }

        return (T)s;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] stringListToArrayType(Class<T> elementType, List<String> values)
    {
        return values.stream()
                .map(s -> stringToType(elementType, s))
                .toArray(size -> (T[])Array.newInstance(elementType, size));
    }
}
