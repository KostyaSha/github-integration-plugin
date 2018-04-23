package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.base.Predicate;
import hudson.model.TaskListener;
import org.kohsuke.github.GHPullRequest;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class SkipFirstRunForPRFilter implements Predicate<GHPullRequest> {
    private final TaskListener logger;
    private final boolean skipFirstRun;

    private SkipFirstRunForPRFilter(TaskListener logger, boolean skipFirstRun) {
        this.logger = logger;
        this.skipFirstRun = skipFirstRun;
    }

    public static Predicate<GHPullRequest> ifSkippedFirstRun(TaskListener logger, boolean skipFirstRun) {
        return new SkipFirstRunForPRFilter(logger, skipFirstRun);
    }

    @Override
    public boolean apply(GHPullRequest remotePR) {
        if (skipFirstRun) {
            logger.getLogger().println(String.format("Skipping first run for PR #%s", remotePR.getNumber()));
            return false;
        }

        return true;
    }
}
