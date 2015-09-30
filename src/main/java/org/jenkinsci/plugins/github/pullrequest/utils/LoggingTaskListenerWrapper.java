package org.jenkinsci.plugins.github.pullrequest.utils;

import hudson.util.StreamTaskListener;
import org.slf4j.Logger;

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

    private final Logger logger;

    public LoggingTaskListenerWrapper(Logger logger, File out, Charset charset) throws IOException {
        super(out, charset);
        this.logger = logger;
    }

    @Override
    public PrintWriter error(String msg) {
        logger.error(msg);
        return super.error(msg);
    }

    @Override
    public PrintWriter error(String format, Object... args) {
        logger.error(format, args);
        return super.error(arrayFormat(format, args).getMessage());
    }
    
    public void info(String format, Object... args) {
        logger.info(format, args);
        getLogger().println(arrayFormat(format, args).getMessage());
    }
 
    public void info(String msg) {
        logger.info(msg);
        getLogger().println(msg);
    }
    
    public void debug(String format, Object... args) {
        logger.debug(format, args);
        getLogger().println(arrayFormat(format, args).getMessage());
    }
 
    public void debug(String msg) {
        logger.debug(msg);
        getLogger().println(msg);
    }
}
