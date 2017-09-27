package io.github.notsyncing.cowherd.server;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class JULBridgeHandler extends Handler {
    @Override
    public void publish(LogRecord record) {
        CowherdLogger logger = CowherdLogger.getInstance(record.getLoggerName());

        if (record.getThrown() != null) {
            logger.log(CowherdLogger.julLevelToLog4jLevel(record.getLevel()), record.getMessage(), record.getThrown());
        } else {
            logger.log(CowherdLogger.julLevelToLog4jLevel(record.getLevel()), record.getMessage());
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
