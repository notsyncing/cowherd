package io.github.notsyncing.cowherd.commons;

import com.alibaba.fastjson.JSON;
import io.github.notsyncing.cowherd.annotations.ConfigField;
import io.github.notsyncing.cowherd.server.CowherdLogger;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 包含全局配置信息
 */
public class CowherdConfiguration
{
    @ConfigField
    private static int listenPort = 8080;

    @ConfigField
    private static Path[] contextRoots;

    @ConfigField
    private static long maxUploadFileSize = 2 * 1024 * 1024;

    @ConfigField
    private static Path uploadCacheDir;

    @ConfigField
    private static String apiServiceRoute = "/api/";

    @ConfigField
    private static String apiServiceDomain;

    @ConfigField("websocket")
    private static WebsocketConfig websocketConfig;

    @ConfigField
    private static Path logDir;

    @ConfigField
    private static boolean everyHtmlIsTemplate = false;

    @ConfigField
    private static String[] allowOrigins;

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
    public static Path[] getContextRoots()
    {
        if (contextRoots == null) {
            try {
                contextRoots = new Path[] { Paths.get(CowherdConfiguration.class.getResource("/APP_ROOT").toURI()) };
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return contextRoots;
    }

    /**
     * 设置上下文路径
     * @param contextRoots 要使用的上下文路径
     */
    public static void setContextRoots(Path[] contextRoots)
    {
        CowherdConfiguration.contextRoots = contextRoots;
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
     * 获取 WebSocket 服务的设置
     * @return WebSocket 服务设置信息
     */
    public static WebsocketConfig getWebsocketConfig()
    {
        return websocketConfig;
    }

    /**
     * 设置 WebSocket 服务
     * @param websocketConfig 要设置的 WebSocket 服务信息
     */
    public static void setWebsocketConfig(WebsocketConfig websocketConfig)
    {
        CowherdConfiguration.websocketConfig = websocketConfig;
    }

    /**
     * 获取日志存放目录
     * @return 日志存放目录
     */
    public static Path getLogDir()
    {
        return logDir;
    }

    /**
     * 设置日志存放目录
     * @param logDir 要使用的日志存放目录
     */
    public static void setLogDir(Path logDir)
    {
        CowherdConfiguration.logDir = logDir;

        if (!Files.exists(logDir)) {
            try {
                Files.createDirectories(logDir);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to create log directory: " + logDir);
            }
        }

        CowherdLogger.loggerConfigChanged();
    }

    public static boolean isEveryHtmlIsTemplate()
    {
        return everyHtmlIsTemplate;
    }

    public static void setEveryHtmlIsTemplate(boolean everyHtmlIsTemplate)
    {
        CowherdConfiguration.everyHtmlIsTemplate = everyHtmlIsTemplate;
    }

    /**
     * 获取允许 CORS 的域名列表
     * @return 允许 CORS 的域名列表
     */
    public static String[] getAllowOrigins()
    {
        return allowOrigins;
    }

    /**
     * 设置允许 CORS 的域名列表
     * @param allowOrigins 允许 CORS 的域名列表
     */
    public static void setAllowOrigins(String[] allowOrigins)
    {
        CowherdConfiguration.allowOrigins = allowOrigins;
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
                Object v;

                if (f.getType().equals(Path.class)) {
                    v = Paths.get(config.getString(name));
                } else if (config.getValue(name).getClass().equals(JsonObject.class)) {
                    v = JSON.parseObject(config.getJsonObject(name).toString(), f.getType());
                } else if (f.getType().isArray()) {
                    List l = JSON.parseArray(config.getJsonArray(name).toString(), f.getType().getComponentType());
                    v = Array.newInstance(f.getType().getComponentType(), l.size());

                    for (int i = 0; i < l.size(); i++) {
                        Array.set(v, i, l.get(i));
                    }
                } else {
                    v = config.getValue(name);
                }

                String setterName = "set" + f.getName()
                        .replaceFirst(f.getName().substring(0, 1), f.getName()
                                .substring(0, 1).toUpperCase());

                try {
                    Method setter = CowherdConfiguration.class.getMethod(setterName, f.getType());
                    setter.invoke(null, v);
                } catch (NoSuchMethodException e) {
                    f.set(null, v);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("CowherdConfiguration: Error occured when reading configuration key " + name + ": " + e.getMessage());
            }
        }

        if (config.containsKey("user")) {
            userConfiguration = config.getJsonObject("user");
        }
    }
}
