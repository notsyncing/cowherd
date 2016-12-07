package io.github.notsyncing.cowherd.models;

import java.net.URISyntaxException;

public class SimpleURI
{
    private String host;
    private int port;
    private String scheme;
    private String path;
    private String query;
    private String fragment;

    public SimpleURI()
    {

    }

    public SimpleURI(String host, int port, String scheme, String path, String query, String fragment)
    {
        this.host = host;
        this.port = port;
        this.scheme = scheme;
        this.path = path;
        this.query = query;
        this.fragment = fragment;
    }

    public SimpleURI(String absoluteURI) throws URISyntaxException
    {
        int i = absoluteURI.indexOf("://");
        int curr = 0;

        if (i < 0) {
            throw new URISyntaxException(absoluteURI, "Cannot parse scheme");
        }

        scheme = absoluteURI.substring(curr, i);
        curr = i + 3;

        i = absoluteURI.indexOf('/', curr);

        if (i < 0) {
            i = absoluteURI.indexOf('?', curr);

            if (i < 0) {
                i = absoluteURI.indexOf('#', curr);

                if (i < 0) {
                    i = absoluteURI.length();
                }
            }
        }

        String hp = absoluteURI.substring(curr, i);
        curr = i;

        if (!hp.isEmpty()) {
            i = hp.indexOf(':');

            if (i > 0) {
                host = hp.substring(0, i);
                port = Integer.parseInt(hp.substring(i + 1));
            } else {
                host = hp;
                port = 80;
            }
        }

        i = absoluteURI.indexOf('?', curr);

        if (i > 0) {
            path = absoluteURI.substring(curr, i);
            query = absoluteURI.substring(i + 1);

            i = query.indexOf('#');

            if (i >= 0) {
                fragment = query.substring(i + 1);
                query = query.substring(0, i);
            }
        } else {
            path = absoluteURI.substring(curr);

            i = path.indexOf('#');

            if (i >= 0) {
                fragment = path.substring(i + 1);
                path = path.substring(0, i);
            }
        }

        if (path.isEmpty()) {
            path = "/";
        }
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getScheme()
    {
        return scheme;
    }

    public void setScheme(String scheme)
    {
        this.scheme = scheme;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public String getFragment()
    {
        return fragment;
    }

    public void setFragment(String fragment)
    {
        this.fragment = fragment;
    }
}
