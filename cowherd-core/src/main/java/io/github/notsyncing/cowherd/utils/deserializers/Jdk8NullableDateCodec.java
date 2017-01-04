package io.github.notsyncing.cowherd.utils.deserializers;

import com.alibaba.fastjson.parser.deserializer.Jdk8DateCodec;
import io.github.notsyncing.cowherd.utils.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Jdk8NullableDateCodec extends Jdk8DateCodec
{
    @Override
    protected LocalDateTime parseDateTime(String text, DateTimeFormatter formatter)
    {
        if (StringUtils.isEmpty(text)) {
            return null;
        }

        return super.parseDateTime(text, formatter);
    }

    @Override
    protected LocalDate parseLocalDate(String text, String format, DateTimeFormatter formatter)
    {
        if (StringUtils.isEmpty(text)) {
            return null;
        }

        return super.parseLocalDate(text, format, formatter);
    }
}
