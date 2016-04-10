package io.github.notsyncing.cowherd.commons;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GlobalStorage
{
    private static int listenPort = 8080;
    private static Path contextRoot;

    private static long maxUploadFileSize = 2 * 1024 * 1024;
    private static Path uploadCacheDir;

    private static String apiServiceRoute = "/api/";
    private static String apiServiceDomain;

    public static int getListenPort()
    {
        return listenPort;
    }

    public static void setListenPort(int listenPort)
    {
        GlobalStorage.listenPort = listenPort;
    }

    public static Path getContextRoot()
    {
        if (contextRoot == null) {
            try {
                contextRoot = Paths.get(GlobalStorage.class.getResource("/APP_ROOT").toURI());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return contextRoot;
    }

    public static void setContextRoot(Path contextRoot)
    {
        GlobalStorage.contextRoot = contextRoot;
    }

    public static long getMaxUploadFileSize()
    {
        return maxUploadFileSize;
    }

    public static void setMaxUploadFileSize(long maxUploadFileSize)
    {
        GlobalStorage.maxUploadFileSize = maxUploadFileSize;
    }

    public static Path getUploadCacheDir() throws IOException
    {
        if (uploadCacheDir == null) {
            uploadCacheDir = Files.createTempDirectory("cowherd_uploads");
        }

        return uploadCacheDir;
    }

    public static void setUploadCacheDir(Path uploadCacheDir)
    {
        GlobalStorage.uploadCacheDir = uploadCacheDir;
    }

    public static String getApiServiceRoute()
    {
        return apiServiceRoute;
    }

    public static void setApiServiceRoute(String apiServiceRoute)
    {
        GlobalStorage.apiServiceRoute = apiServiceRoute;
    }

    public static String getApiServiceDomain()
    {
        return apiServiceDomain;
    }

    public static void setApiServiceDomain(String apiServiceDomain)
    {
        GlobalStorage.apiServiceDomain = apiServiceDomain;
    }
}
