package io.github.notsyncing.cowherd.files;

import io.github.notsyncing.cowherd.annotations.Component;
import io.github.notsyncing.cowherd.models.UploadFileInfo;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件存储对象
 * 用于方便分类存储各类文件
 */
public class FileStorage
{
    private Map<Enum, Path> storagePaths = new ConcurrentHashMap<>();
    private FileSystem fs;

    public FileStorage(Vertx vertx)
    {
        fs = vertx.fileSystem();
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
            System.out.println("FileStorage: Tag " + tag + " already registered to path " + storagePaths.get(tag) +
                    ", will be overwritten to " + path);
        }

        storagePaths.put(tag, path);

        if (!Files.exists(path)) {
            Path p = Files.createDirectories(path);
            System.out.println("FileStorage: Created storage path " + p + " for tag " + tag);
        } else {
            System.out.println("FileStorage: Registered storage path " + path + " to tag " + tag);
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
    public CompletableFuture<Path> storeFile(Path file, Enum tag, String newFileName, boolean noRemoveOld)
    {
        CompletableFuture<Path> f = new CompletableFuture<>();

        Path store = storagePaths.get(tag);
        Path to = store.resolve(newFileName == null ? file.getFileName().toString() : newFileName);

        fs.copy(file.toString(), to.toString(), r -> {
            if (r.succeeded()) {
                if (noRemoveOld) {
                    f.complete(store.relativize(to));
                } else {
                    fs.delete(file.toString(), r2 -> {
                        if (r2.succeeded()) {
                            f.complete(to);
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
}
