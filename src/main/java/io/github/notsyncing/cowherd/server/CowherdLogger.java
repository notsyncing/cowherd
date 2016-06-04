package io.github.notsyncing.cowherd.server;

import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CowherdLogger
{
    private static CowherdLogger instance = new CowherdLogger();

    private LoggerContext context;
    private Map<Enum, Logger> loggers = new ConcurrentHashMap<>();
    private Logger log;

    private CowherdLogger()
    {
        context = LoggerContext.getContext();
    }

    public static CowherdLogger getInstance()
    {
        return instance;
    }

    public static CowherdLogger getInstance(Enum tag, Object o)
    {
        instance.registerTag(tag, o.getClass().getSimpleName());
        return instance;
    }

    public void loggerConfigChanged()
    {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setConfigurationName("RollingBuilder");

        AppenderComponentBuilder appenderBuilder = builder.newAppender("stdout", "CONSOLE").addAttribute("target",
                ConsoleAppender.Target.SYSTEM_OUT);
        appenderBuilder.add(builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d %c %level %msg%n%throwable"));
        builder.add(appenderBuilder);

        if (CowherdConfiguration.getLogDir() != null) {
            LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
                    .addAttribute("pattern", "%d %c %level %msg%n");
            ComponentBuilder triggeringPolicy = builder.newComponent("Policies")
                    .addComponent(builder.newComponent("CronTriggeringPolicy").addAttribute("schedule", "0 0 0 * * ?"));

            appenderBuilder = builder.newAppender("rolling", "RollingFile")
                    .addAttribute("fileName", CowherdConfiguration.getLogDir().toAbsolutePath().resolve("cowherd.log"))
                    .addAttribute("filePattern", CowherdConfiguration.getLogDir().toAbsolutePath().resolve("cowherd-{yyyy-MM-dd}.log.gz"))
                    .add(layoutBuilder)
                    .addComponent(triggeringPolicy);
            builder.add(appenderBuilder);

            builder.add(builder.newRootLogger(Level.DEBUG)
                    .add(builder.newAppenderRef("rolling"))
                    .add(builder.newAppenderRef("stdout")));
        }

        context.updateLoggers(builder.build());

        for (Enum key : loggers.keySet()) {
            loggers.replace(key, context.getLogger(loggers.get(key).getName()));
        }

        log = context.getLogger(getClass().getSimpleName());
        log.info("Log configuration reloaded.");
    }

    public void registerTag(Enum tag, String text)
    {
        loggers.put(tag, context.getLogger(text));
    }

    public void registerTag(Enum tag, Class clazz)
    {
        registerTag(tag, clazz.getSimpleName());
    }

    public void log(Enum tag, Level level, String message)
    {
        loggers.get(tag).log(level, message);
    }

    public void log(Enum tag, Level level, String message, Throwable ex)
    {
        loggers.get(tag).log(level, message, ex);
    }
}
