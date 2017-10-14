package io.github.notsyncing.cowherd.models;

import io.github.notsyncing.cowherd.Cowherd;
import io.github.notsyncing.cowherd.files.FileStorage;
import io.github.notsyncing.cowherd.utils.FutureUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * 上传文件信息类
 */
public class UploadFileInfo
{
    private String filename;
    private File file;
    private String parameterName;

    /**
     * 获取上传文件的文件名
     * @return 文件名，若无文件名，则返回 null
     */
    public String getFilename()
    {
        return filename;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    /**
     * 获取上传的文件对象
     * @return 文件对象
     */
    public File getFile()
    {
        return file;
    }

    public void setFile(File file)
    {
        this.file = file;
    }

    /**
     * 获取上传文件的表单参数名称
     * @return 表单参数名称
     */
    public String getParameterName()
    {
        return parameterName;
    }

    public void setParameterName(String parameterName)
    {
        this.parameterName = parameterName;
    }

    private FileStorage getFileStorage() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        return Cowherd.dependencyInjector.getComponent(FileStorage.class);
    }

    public CompletableFuture<Path> store(Enum tag, String newFileName, boolean noRemoveOld) {
        try {
            FileStorage fs = getFileStorage();

            return fs.storeFile(this, tag, newFileName, noRemoveOld)
                    .thenApply(p -> fs.relativize(tag, p));
        } catch (Exception e) {
            return FutureUtils.failed(e);
        }
    }

    public CompletableFuture<Path> store(Enum tag) {
        try {
            FileStorage fs = getFileStorage();

            return fs.storeFile(this, tag)
                    .thenApply(p -> fs.relativize(tag, p));
        } catch (Exception e) {
            return FutureUtils.failed(e);
        }
    }

    public CompletableFuture<Path> storeWithRandomName(Enum tag) {
        try {
            FileStorage fs = getFileStorage();

            return fs.storeFileWithRandomName(this, tag)
                    .thenApply(p -> fs.relativize(tag, p));
        } catch (Exception e) {
            return FutureUtils.failed(e);
        }
    }
}
