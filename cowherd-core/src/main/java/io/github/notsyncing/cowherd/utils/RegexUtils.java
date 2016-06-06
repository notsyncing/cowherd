package io.github.notsyncing.cowherd.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils
{
    static Pattern groupNamePattern = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

    public static List<String> getGroupNames(Pattern p)
    {
        String s = p.pattern();
        Matcher m = groupNamePattern.matcher(s);
        List<String> names = new ArrayList<>();

        while (m.find()) {
            names.add(m.group(1));
        }

        return names;
    }

    public static void addMatchedGroupsToMap(String s, Pattern p, Map<String, List<String>> map)
    {
        Matcher domainMatcher = p.matcher(s);

        if (domainMatcher.find()) {
            for (String n : getGroupNames(p)) {
                if (map.containsKey(n)) {
                    map.get(n).add(domainMatcher.group(n));
                } else {
                    List<String> l = new ArrayList<>();
                    l.add(domainMatcher.group(n));
                    map.put(n, l);
                }
            }
        }
    }
}
