package io.github.notsyncing.cowherd.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

import java.io.IOException;
import java.io.InputStream;

public class FileUtils
{
    public static String getInternalResourceAsString(String name) throws IOException
    {
        return StringUtils.streamToString(FileUtils.class.getResourceAsStream(name));
    }

    public static int pumpInputStreamToWriteStream(InputStream input, long skip, long length, WriteStream<Buffer> output) throws IOException
    {
        byte[] readBuf = new byte[10240];
        int realLength = 0;
        int l;

        if (skip > 0) {
            input.skip(skip);
        }

        while ((l = input.read(readBuf)) != -1) {
            realLength += l;
            output.write(Buffer.buffer().appendBytes(readBuf, 0, l));

            if (realLength >= length) {
                break;
            }
        }

        return realLength;
    }

    public static String guessContentType(String filename)
    {
        if (filename.endsWith(".css")) {
            return "text/css";
        }

        return null;
    }
}
