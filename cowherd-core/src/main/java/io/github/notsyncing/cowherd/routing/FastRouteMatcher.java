package io.github.notsyncing.cowherd.routing;

import io.github.notsyncing.cowherd.models.Pair;
import io.github.notsyncing.cowherd.models.RouteInfo;
import io.github.notsyncing.cowherd.models.SimpleURI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FastRouteMatcher extends RouteMatcher
{
    private String[] currUriParts;

    public FastRouteMatcher(SimpleURI uri)
    {
        super(uri);

        String[] uriParts = uri.getPath().split("/");
        currUriParts = Stream.of(uriParts)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    protected MatchedRoute match(RouteInfo route, boolean matchOnly)
    {
        if (("/".equals(uri.getPath())) && (route.isEntry())) {
            MatchedRoute mr = new MatchedRoute();
            mr.setRoute(route);
            return mr;
        }

        String[] currRouteParts = route.getDissolvedPath();

        int i = 0;

        List<Pair<String, String>> params = new ArrayList<>();

        while (i < currRouteParts.length) {
            if (currRouteParts[i].startsWith("**:")) {
                String param = currRouteParts[i].substring(3);
                params.add(new Pair<>(param, Arrays.stream(currUriParts)
                        .skip(i)
                        .collect(Collectors.joining("/"))));
                break;
            } else if (currRouteParts[i].startsWith("*")) {
                i++;
                continue;
            } else if (currRouteParts[i].startsWith(":")) {
                String param = currRouteParts[i].substring(1);
                params.add(new Pair<>(param, currUriParts[i]));
            } else if (!currRouteParts[i].equals(currUriParts[i])) {
                return null;
            }

            i++;
        }

        MatchedRoute mr = new MatchedRoute(params);
        mr.setRoute(route);

        return mr;
    }
}
