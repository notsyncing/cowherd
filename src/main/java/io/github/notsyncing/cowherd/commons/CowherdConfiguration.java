package io.github.notsyncing.cowherd.commons;

import io.github.notsyncing.cowherd.annotations.ConfigField;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 包含全局配置信息
 */
public class CowherdConfiguration
{
    @ConfigField
    private static int listenPort = 8080;

    @ConfigField
    private static Path contextRoot;

    @ConfigField
    private static long maxUploadFileSize = 2 * 1024 * 1024;

    @ConfigField
    private static Path uploadCacheDir;

    @ConfigField
    private static String apiServiceRoute = "/api/";

    @ConfigField
    private static String apiServiceDomain;

    private static JsonObject userConfiguration;

    /**
     * 获取当前监听的端口号
     * @return 当前监听的端口号
     */
    public static int getListenPort()
    {
        return listenPort;
    }

    /**
     * 设置当前监听的端口号
     * @param listenPort 要监听的端口号
     */
    public static void setListenPort(int listenPort)
    {
        CowherdConfiguration.listenPort = listenPort;
    }

    /**
     * 获取上下文路径
     * @return 上下文路径
     */
    public static Path getContextRoot()
    {
        if (contextRoot == null) {
            try {
                contextRoot = Paths.get(CowherdConfiguration.class.getResource("/APP_ROOT").toURI());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return contextRoot;
    }

    /**
     * 设置上下文路径
     * @param contextRoot 要使用的上下文路径
     */
    public static void setContextRoot(Path contextRoot)
    {
        CowherdConfiguration.contextRoot = contextRoot;
    }

    /**
     * 获取允许上传文件的最大长度
     * @return 允许上传文件的最大长度
     */
    public static long getMaxUploadFileSize()
    {
        return maxUploadFileSize;
    }

    /**
     * 设置允许上传文件的最大长度
     * @param maxUploadFileSize 允许上传文件的最大长度
     */
    public static void setMaxUploadFileSize(long maxUploadFileSize)
    {
        CowherdConfiguration.maxUploadFileSize = maxUploadFileSize;
    }

    /**
     * 获取上传文件的临时存放路径
     * @return 上传文件的临时存放路径
     * @throws IOException
     */
    public static Path getUploadCacheDir() throws IOException
    {
        if (uploadCacheDir == null) {
            uploadCacheDir = Files.createTempDirectory("cowherd_uploads");
        }

        return uploadCacheDir;
    }

    /**
     * 设置上传文件的临时存放路径
     * @param uploadCacheDir 上传文件的临时存放路径
     */
    public static void setUploadCacheDir(Path uploadCacheDir)
    {
        CowherdConfiguration.uploadCacheDir = uploadCacheDir;
    }

    /**
     * 获取 API 服务的请求路径
     * @return API 服务的请求路径
     */
    public static String getApiServiceRoute()
    {
        return apiServiceRoute;
    }

    /**
     * 设置 API 服务的请求路径
     * @param apiServiceRoute API 服务的请求路径
     */
    public static void setApiServiceRoute(String apiServiceRoute)
    {
        CowherdConfiguration.apiServiceRoute = apiServiceRoute;
    }

    /**
     * 获取 API 服务的请求域名
     * @return API 服务的请求域名
     */
    public static String getApiServiceDomain()
    {
        return apiServiceDomain;
    }

    /**
     * 设置 API 服务的请求域名
     * @param apiServiceDomain API 服务的请求域名
     */
    public static void setApiServiceDomain(String apiServiceDomain)
    {
        CowherdConfiguration.apiServiceDomain = apiServiceDomain;
    }

    /**
     * 获取配置文件中的用户自定义配置项
     * @return 用户配置
     */
    public static JsonObject getUserConfiguration()
    {
        return userConfiguration;
    }

    public static void fromConfig(JsonObject config)
    {
        for (Field f : CowherdConfiguration.class.getDeclaredFields()) {
            if (!f.isAnnotationPresent(ConfigField.class)) {
                continue;
            }

            String name = f.getAnnotation(ConfigField.class).value();

            if (name.isEmpty()) {
                name = f.getName();
            }

            if (!config.containsKey(name)) {
                continue;
            }

            try {
                if (f.getType().equals(Path.class)) {
                    f.set(null, Paths.get(config.getString(name)));
                } else {
                    f.set(null, config.getValue(name));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                System.out.println("CowherdConfiguration: Error occured when reading configuration key " + name + ": " + e.getMessage());
            }
        }

        if (config.containsKey("user")) {
            userConfiguration = config.getJsonObject("user");
        }
    }
}
