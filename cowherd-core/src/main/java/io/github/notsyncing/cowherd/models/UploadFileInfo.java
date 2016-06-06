package io.github.notsyncing.cowherd.models;

import java.io.File;

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
}
