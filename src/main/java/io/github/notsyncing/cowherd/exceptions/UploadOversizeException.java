package io.github.notsyncing.cowherd.exceptions;

import io.github.notsyncing.cowherd.commons.GlobalStorage;

public class UploadOversizeException extends Exception
{
    String filename;

    public UploadOversizeException(String filename)
    {
        super("Upload file '" + filename + "' is oversize, max size = " + GlobalStorage.getMaxUploadFileSize());

        this.filename = filename;
    }
}
