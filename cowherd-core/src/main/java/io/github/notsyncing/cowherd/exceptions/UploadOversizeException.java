package io.github.notsyncing.cowherd.exceptions;

import io.github.notsyncing.cowherd.commons.CowherdConfiguration;

/**
 * 上传文件长度超过限额时发生的异常
 */
public class UploadOversizeException extends Exception
{
    String filename;

    public UploadOversizeException(String filename)
    {
        super("Upload file '" + filename + "' is oversize, max size = " + CowherdConfiguration.getMaxUploadFileSize());

        this.filename = filename;
    }
}
