package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.base.Predicate;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import static java.util.Objects.isNull;

/**
 * @author Kanstantsin Shautsou
 */
public class SkipPRInBadState implements Predicate<GHPullRequest>, java.util.function.Predicate<GHPullRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(NotUpdatedPRFilter.class);

    private final GitHubPRRepository localRepo;
    private final TaskListener logger;

    private SkipPRInBadState(GitHubPRRepository localRepo, TaskListener logger) {
        this.localRepo = localRepo;
        this.logger = logger;
    }

    public static SkipPRInBadState badState(GitHubPRRepository localRepo, TaskListener logger) {
        return new SkipPRInBadState(localRepo, logger);
    }

    @Override
    public boolean apply(@Nullable GHPullRequest remotePR) {
        if (isNull(remotePR)) {
            return true;
        }

        @CheckForNull GitHubPRPullRequest localPR = localRepo.getPulls().get(remotePR.getNumber());

        if (localPR != null && localPR.isInBadState()) {
            logger.error("local PR [#{} {}] is in bad state", remotePR.getNumber(), remotePR.getTitle());
            return false;
        }

        return true;
    }

    @Override
    public boolean test(GHPullRequest remotePR) {
        return apply(remotePR);
    }
}
