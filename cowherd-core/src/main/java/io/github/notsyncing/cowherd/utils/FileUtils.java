package io.github.notsyncing.cowherd.utils;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.spi.BufferFactory;
import io.vertx.core.streams.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class FileUtils
{
    public static String getInternalResourceAsString(String name) throws IOException
    {
        return StringUtils.streamToString(FileUtils.class.getResourceAsStream(name));
    }

    public static int pumpInputStreamToWriteStream(InputStream input, WriteStream<Buffer> output) throws IOException
    {
        byte[] buf = new byte[10240];
        int length = 0;
        int l;

        while ((l = input.read(buf)) != -1) {
            length += l;
            output.write(Buffer.buffer().appendBytes(buf, 0, l));
        }

        return length;
    }

    public static String guessContentType(String filename)
    {
        if (filename.endsWith(".css")) {
            return "text/css";
        }

        return null;
    }
}
