package io.github.notsyncing.cowherd.server;

import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CowherdLogger
{
    private static LoggerContext context = LoggerContext.getContext();
    private static Map<String, Logger> loggers = new ConcurrentHashMap<>();
    private static Logger log;

    private String tag;

    private CowherdLogger(String tag)
    {
        this.tag = tag;
    }

    public static CowherdLogger getInstance(Object o)
    {
        CowherdLogger.registerTag(o.getClass().getSimpleName());

        return new CowherdLogger(o.getClass().getSimpleName());
    }

    public static CowherdLogger getInstance(Class c)
    {
        CowherdLogger.registerTag(c.getSimpleName());

        return new CowherdLogger(c.getSimpleName());
    }

    public static CowherdLogger getAccessLogger()
    {
        return new CowherdLogger("AccessLogger");
    }

    public static void loggerConfigChanged()
    {
        Level level = CowherdConfiguration.isVerbose() ? Level.DEBUG : Level.INFO;

        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setConfigurationName("RollingBuilder");

        LayoutComponentBuilder logStyle = builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d %t.%c %level %msg%n%throwable");
        AppenderComponentBuilder appenderBuilder = builder.newAppender("stdout", "CONSOLE")
                .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
        appenderBuilder.add(logStyle);
        builder.add(appenderBuilder);

        RootLoggerComponentBuilder rootLoggerBuilder = builder.newRootLogger(level)
                .add(builder.newAppenderRef("stdout"));

        if (CowherdConfiguration.getLogDir() != null) {
            ComponentBuilder triggeringPolicy = builder.newComponent("Policies")
                    .addComponent(builder.newComponent("TimeBasedTriggeringPolicy")
                            .addAttribute("modulate", true)
                            .addAttribute("interval", 1));

            appenderBuilder = builder.newAppender("rollingCowherd", "RollingFile")
                    .addAttribute("fileName",
                            CowherdConfiguration.getLogDir().toAbsolutePath().resolve("cowherd.log").toString())
                    .addAttribute("filePattern",
                            CowherdConfiguration.getLogDir().toAbsolutePath().resolve("cowherd-%d{yyyy-MM-dd}.log").toString())
                    .add(logStyle)
                    .addComponent(triggeringPolicy);
            builder.add(appenderBuilder);

            rootLoggerBuilder.add(builder.newAppenderRef("rollingCowherd"));

            appenderBuilder = builder.newAppender("rollingAccess", "RollingFile")
                    .addAttribute("fileName",
                            CowherdConfiguration.getLogDir().toAbsolutePath().resolve("access.log").toString())
                    .addAttribute("filePattern",
                            CowherdConfiguration.getLogDir().toAbsolutePath().resolve("access-%d{yyyy-MM-dd}.log").toString())
                    .add(logStyle)
                    .addComponent(triggeringPolicy);
            builder.add(appenderBuilder);

            LoggerComponentBuilder accessLoggerBuilder = builder.newLogger("AccessLogger", level)
                    .add(builder.newAppenderRef("rollingAccess"))
                    .add(builder.newAppenderRef("stdout"))
                    .addAttribute("additivity", false);

            builder.add(accessLoggerBuilder);
        }

        builder.add(rootLoggerBuilder);

        Configuration conf = builder.build();

        context.stop();
        context.updateLoggers(conf);
        context.start(conf);

        loggers.put("AccessLogger", context.getLogger("AccessLogger"));

        log = context.getLogger(CowherdLogger.class.getSimpleName());
        log.info("Log configuration reloaded.");
    }

    public static void registerTag(String tag)
    {
        loggers.put(tag, context.getLogger(tag));
    }

    public static void registerTag(Class clazz)
    {
        registerTag(clazz.getSimpleName());
    }

    public void log(Level level, String message)
    {
        loggers.get(tag).log(level, message);
    }

    public void log(Level level, String message, Throwable ex)
    {
        loggers.get(tag).log(level, message, ex);
    }

    public void i(String message)
    {
        log(Level.INFO, message);
    }

    public void w(String message)
    {
        log(Level.WARN, message);
    }

    public void w(String message, Throwable ex)
    {
        log(Level.WARN, message, ex);
    }

    public void e(String message)
    {
        log(Level.ERROR, message);
    }

    public void e(String message, Throwable ex)
    {
        log(Level.ERROR, message, ex);
    }

    public void t(String message)
    {
        log(Level.TRACE, message);
    }

    public void t(String message, Throwable ex)
    {
        log(Level.TRACE, message, ex);
    }

    public void f(String message)
    {
        log(Level.FATAL, message);
    }

    public void f(String message, Throwable ex)
    {
        log(Level.FATAL, message, ex);
    }

    public void d(String message)
    {
        log(Level.DEBUG, message);
    }

    public void d(String message, Throwable ex)
    {
        log(Level.DEBUG, message, ex);
    }
}
