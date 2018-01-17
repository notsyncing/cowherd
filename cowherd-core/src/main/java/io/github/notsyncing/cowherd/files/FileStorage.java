package io.github.notsyncing.cowherd.files;

import io.github.notsyncing.cowherd.Cowherd;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.commons.RouteType;
import io.github.notsyncing.cowherd.models.ActionMethodInfo;
import io.github.notsyncing.cowherd.models.RouteInfo;
import io.github.notsyncing.cowherd.models.UploadFileInfo;
import io.github.notsyncing.cowherd.routing.RouteManager;
import io.github.notsyncing.cowherd.server.CowherdLogger;
import io.github.notsyncing.cowherd.utils.StringUtils;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 文件存储对象
 * 用于方便分类存储各类文件
 */
public class FileStorage
{
    private Map<Enum, Path> storagePaths = new ConcurrentHashMap<>();
    private FileSystem fs;
    private CowherdLogger log = CowherdLogger.getInstance(this);

    public FileStorage(Vertx vertx)
    {
        init(vertx);
    }

    public FileStorage() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        this(Cowherd.dependencyInjector.getComponent(Vertx.class));
    }

    protected void init(Vertx vertx) {
        try {
            fs = vertx.fileSystem();
        } catch (Exception e) {
            log.e("Failed to create file storage", e);
        }
    }

    /**
     * 注册一个文件存储目录
     * @param tag 标识该存储类型的枚举
     * @param path 要注册的目录
     * @throws IOException
     */
    public void registerStoragePath(Enum tag, String path) throws IOException
    {
        registerStoragePath(tag, Paths.get(path));
    }

    /**
     * 注册一个文件存储目录
     * @param tag 标识该存储类型的枚举
     * @param path 要注册的目录
     * @throws IOException
     */
    public void registerStoragePath(Enum tag, Path path) throws IOException
    {
        if (storagePaths.containsKey(tag)) {
            log.w("Tag " + tag + " already registered to path " + storagePaths.get(tag) +
                    ", will be overwritten to " + path);
        }

        storagePaths.put(tag, path);

        if (!Files.exists(path)) {
            Path p = Files.createDirectories(path);
            log.i("Created storage path " + p + " for tag " + tag);
        } else {
            log.i("Registered storage path " + path + " to tag " + tag);
        }
    }

    /**
     * 获取存储类别标识所对应的存放目录
     * @param tag 存储类别标识枚举
     * @return 该类别标识所对应的存放目录
     */
    public Path getStoragePath(Enum tag)
    {
        return storagePaths.get(tag);
    }

    /**
     * 异步将文件存放至指定的存储类别中
     * @param file 要存放的文件
     * @param tag 存储类别标识枚举
     * @param newFileName 新文件名，若为 null，则按原文件名存储
     * @param noRemoveOld 为 true 则不删除源文件
     * @return 指示存放是否完成的 CompletableFuture 对象，并包含文件相对于该分类存储目录的相对路径
     */
    public CompletableFuture<Path> storeFile(Path file, Enum tag, String newFileName, boolean noRemoveOld) {
        CompletableFuture<Path> f = new CompletableFuture<>();

        String fileName = newFileName == null ? file.getFileName().toString() : newFileName;
        Path store = storagePaths.get(tag);
        Path to;

        if (store == null) {
            f.completeExceptionally(new Exception("Storage tag " + tag + " not registered!"));
            return f;
        }

        if (CowherdConfiguration.isStoreFilesByDate()) {
            LocalDate date = LocalDate.now();
            to = store.resolve(String.valueOf(date.getYear())).resolve(String.valueOf(date.getMonthValue()))
                    .resolve(String.valueOf(date.getDayOfMonth()));

            try {
                Files.createDirectories(to);
            } catch (Exception e) {
                f.completeExceptionally(e);
                return f;
            }

            to = to.resolve(fileName);
        } else {
            to = store.resolve(fileName);
        }

        final Path finalTo = to;

        fs.copy(file.toString(), to.toString(), r -> {
            if (r.succeeded()) {
                if (noRemoveOld) {
                    f.complete(store.relativize(finalTo));
                } else {
                    fs.delete(file.toString(), r2 -> {
                        if (r2.succeeded()) {
                            f.complete(finalTo);
                        } else {
                            f.completeExceptionally(r2.cause());
                        }
                    });
                }
            } else {
                f.completeExceptionally(r.cause());
            }
        });

        return f;
    }

    /**
     * 异步将文件存放至指定的存储类别中
     * @param file 要存放的文件
     * @param tag 存储类别标识枚举
     * @param newFileName 新文件名，若为 null，则按原文件名存储
     * @param noRemoveOld 为 true 则不删除源文件
     * @return 指示存放是否完成的 CompletableFuture 对象，并包含文件相对于该分类存储目录的相对路径
     */
    public CompletableFuture<Path> storeFile(File file, Enum tag, String newFileName, boolean noRemoveOld)
    {
        return storeFile(file.toPath(), tag, newFileName, noRemoveOld);
    }

    /**
     * 异步将文件存放至指定的存储类别中
     * @param file 要存放的文件
     * @param tag 存储类别标识枚举
     * @param newFileName 新文件名，若为 null，则按原文件名存储
     * @param noRemoveOld 为 true 则不删除源文件
     * @return 指示存放是否完成的 CompletableFuture 对象，并包含文件相对于该分类存储目录的相对路径
     */
    public CompletableFuture<Path> storeFile(String file, Enum tag, String newFileName, boolean noRemoveOld)
    {
        return storeFile(Paths.get(file), tag, newFileName, noRemoveOld);
    }

    /**
     * 异步将上传的文件存放至指定的存储类别中
     * @param file 要存放的上传文件信息对象
     * @param tag 存储类别标识枚举
     * @param newFileName 新文件名，若为 null，则按原文件名存储
     * @param noRemoveOld 为 true 则不删除源文件
     * @return 指示存放是否完成的 CompletableFuture 对象，并包含文件相对于该分类存储目录的相对路径
     */
    public CompletableFuture<Path> storeFile(UploadFileInfo file, Enum tag, String newFileName, boolean noRemoveOld)
    {
        if (file == null) {
            return CompletableFuture.completedFuture(null);
        }

        return storeFile(file.getFile(), tag, newFileName, noRemoveOld);
    }

    /**
     * 异步将上传的文件按源文件名存放至指定的存储类别中，并删除源文件
     * @param file 要存放的上传文件信息对象
     * @param tag 存储类别标识枚举
     * @return 指示存放是否完成的 CompletableFuture 对象，并包含文件相对于该分类存储目录的相对路径
     */
    public CompletableFuture<Path> storeFile(UploadFileInfo file, Enum tag)
    {
        if (file == null) {
            return CompletableFuture.completedFuture(null);
        }

        if ((StringUtils.isEmpty(file.getFilename())) && ((file.getFile() == null) || (file.getFile().length() <= 0))) {
            return CompletableFuture.completedFuture(null);
        }

        return storeFile(file.getFile(), tag, file.getFilename(), false);
    }

    /**
     * 异步将上传的文件以随机文件名（保持扩展名）存放至指定的存储类别中，并删除源文件
     * @param file 要存放的上传文件信息对象
     * @param tag 存储类别标识枚举
     * @return 指示存放是否完成的 CompletableFuture 对象，并包含文件相对于该分类存储目录的相对路径
     */
    public CompletableFuture<Path> storeFileWithRandomName(UploadFileInfo file, Enum tag)
    {
        if (file == null) {
            return CompletableFuture.completedFuture(null);
        }

        if ((StringUtils.isEmpty(file.getFilename())) && ((file.getFile() == null) || (file.getFile().length() <= 0))) {
            return CompletableFuture.completedFuture(null);
        }

        String fn = file.getFilename();
        int e = fn.lastIndexOf('.');
        String ext = e > 0 ? fn.substring(e) : "";
        String filename = UUID.randomUUID().toString() + ext;

        return storeFile(file.getFile(), tag, filename, false);
    }

    /**
     * 获取文件在某一存储类别中的完整路径
     * @param tag 存储类别标识枚举
     * @param file 要获取完整路径的文件
     * @return 该文件的完整路径
     */
    public Path resolveFile(Enum tag, Path file)
    {
        return storagePaths.get(tag).resolve(file);
    }

    /**
     * 获取文件中某一存储类别中的相对路径
     * @param tag 存储类别标识枚举
     * @param file 要获取相对路径的文件
     * @return 该文件的相对路径
     */
    public Path relativize(Enum tag, Path file)
    {
        return getStoragePath(tag).relativize(file);
    }

    private void addServerRoute(RouteInfo route)
    {
        Method m;

        try {
            m = CowherdFileStorageService.class.getMethod("getFile", Enum.class, String.class);
        } catch (NoSuchMethodException e) {
            log.e("No action for file storage!", e);
            return;
        }

        RouteManager.addRoute(route, new ActionMethodInfo(m));
    }

    /**
     * 注册一条直接访问指定文件存储的路由
     * @param tag 存储类别标识枚举
     * @param routeRegex 路由规则，必须包含一个名为 path 的命名匹配组，用于匹配要访问的文件的相对路径
     */
    public void registerServerRoute(Enum tag, String routeRegex)
    {
        RouteInfo info = new RouteInfo();
        info.setPath(routeRegex);
        info.setType(RouteType.Http);
        info.setOtherParameters(new Object[] { tag });

        addServerRoute(info);
    }

    public void registerServerSimpleRoute(Enum tag, String route)
    {
        RouteInfo info = new RouteInfo();
        info.setPath(route);
        info.setType(RouteType.Http);
        info.setOtherParameters(new Object[] { tag });
        info.setFastRoute(true);

        addServerRoute(info);
    }

    public void removeStoragePathIf(Predicate<Enum> predicate) {
        storagePaths.entrySet().removeIf(e -> predicate.test(e.getKey()));
    }
}
