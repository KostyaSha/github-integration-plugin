package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.base.Predicate;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @author Kanstantsin Shautsou
 */
public class SkipPRInBadState implements Predicate<GHPullRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(NotUpdatedPRFilter.class);

    private final GitHubPRRepository localRepo;
    private final LoggingTaskListenerWrapper logger;

    private SkipPRInBadState(GitHubPRRepository localRepo, LoggingTaskListenerWrapper logger) {
        this.localRepo = localRepo;
        this.logger = logger;
    }

    public static SkipPRInBadState badState(GitHubPRRepository localRepo, LoggingTaskListenerWrapper logger) {
        return new SkipPRInBadState(localRepo, logger);
    }

    @Override
    public boolean apply(@Nullable GHPullRequest remotePR) {
        if (remotePR == null) {
            return true;
        }

        @CheckForNull GitHubPRPullRequest localPR = localRepo.getPulls().get(remotePR.getNumber());

        if (localPR != null && localPR.isInBadState()) {
            logger.error("local PR [#{} {}] is in bad state", remotePR.getNumber(), remotePR.getTitle());
            return false;
        }

        return true;
    }
}
