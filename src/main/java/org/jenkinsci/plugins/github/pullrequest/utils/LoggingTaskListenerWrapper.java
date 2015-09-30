package org.jenkinsci.plugins.github.pullrequest.utils;

import hudson.util.StreamTaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import static org.slf4j.helpers.MessageFormatter.arrayFormat;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class LoggingTaskListenerWrapper extends StreamTaskListener implements Closeable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingTaskListenerWrapper.class);

    public LoggingTaskListenerWrapper(File out, Charset charset) throws IOException {
        super(out, charset);
    }

    @Override
    public PrintWriter error(String msg) {
        LOGGER.error(msg);
        return super.error(msg);
    }

    @Override
    public PrintWriter error(String format, Object... args) {
        LOGGER.error(format, args);
        return super.error(arrayFormat(format, args).getMessage());
    }

    public void info(String format, Object... args) {
        LOGGER.info(format, args);
        getLogger().println(arrayFormat(format, args).getMessage());
    }

    public void info(String msg) {
        LOGGER.info(msg);
        getLogger().println(msg);
    }

    public void debug(String format, Object... args) {
        LOGGER.debug(format, args);
        getLogger().println(arrayFormat(format, args).getMessage());
    }

    public void debug(String msg) {
        LOGGER.debug(msg);
        getLogger().println(msg);
    }
}
