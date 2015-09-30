package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.base.Predicate;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHPullRequest;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class SkipFirstRunForPRFilter implements Predicate<GHPullRequest> {
    private final LoggingTaskListenerWrapper logger;
    private final boolean skipFirstRun;

    private SkipFirstRunForPRFilter(LoggingTaskListenerWrapper logger, boolean skipFirstRun) {
        this.logger = logger;
        this.skipFirstRun = skipFirstRun;
    }

    public static Predicate<GHPullRequest> ifSkippedFirstRun(LoggingTaskListenerWrapper logger, boolean skipFirstRun) {
        return new SkipFirstRunForPRFilter(logger, skipFirstRun);
    }

    @Override
    public boolean apply(GHPullRequest remotePR) {
        if (skipFirstRun) {
            logger.info("Skipping first run for PR #{}", remotePR.getNumber());
            return false;
        }

        return true;
    }
}
