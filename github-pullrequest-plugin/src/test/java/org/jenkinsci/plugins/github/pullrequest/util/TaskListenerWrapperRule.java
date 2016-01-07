package org.jenkinsci.plugins.github.pullrequest.util;

import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.junit.rules.TemporaryFolder;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Rule with provides task listener with logging to file and to logger
 *
 * @author lanwen (Merkushev Kirill)
 */
public class TaskListenerWrapperRule extends TemporaryFolder {
    LoggingTaskListenerWrapper listener;

    @Override
    protected void before() throws Throwable {
        super.before();
        listener = new LoggingTaskListenerWrapper(newFile(), UTF_8);
    }

    public LoggingTaskListenerWrapper getListener() {
        return listener;
    }
}