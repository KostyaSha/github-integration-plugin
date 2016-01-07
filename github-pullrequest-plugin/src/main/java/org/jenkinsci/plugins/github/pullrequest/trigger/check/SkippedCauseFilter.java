package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.base.Predicate;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class SkippedCauseFilter implements Predicate<GitHubPRCause> {
    private final LoggingTaskListenerWrapper listener;

    public SkippedCauseFilter(LoggingTaskListenerWrapper listener) {
        this.listener = listener;
    }

    @Override
    public boolean apply(GitHubPRCause cause) {
        if (cause.isSkip()) {
            listener.debug("Skipping PR #{}", cause.getNumber());
            return false;
        } else {
            listener.debug("Prepare to trigger build for PR #{}, because {}", cause.getNumber(), cause.getReason());
            return true;
        }
    }
}
