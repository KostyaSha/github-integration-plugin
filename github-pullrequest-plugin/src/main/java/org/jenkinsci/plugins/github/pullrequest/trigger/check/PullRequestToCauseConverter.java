package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class PullRequestToCauseConverter implements Function<GHPullRequest, GitHubPRCause> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PullRequestToCauseConverter.class);

    private final GitHubPRRepository localRepo;
    private final LoggingTaskListenerWrapper listener;
    private final GitHubPRTrigger trigger;

    private PullRequestToCauseConverter(GitHubPRRepository localRepo,
                                        LoggingTaskListenerWrapper listener,
                                        GitHubPRTrigger trigger) {
        this.localRepo = localRepo;
        this.listener = listener;
        this.trigger = trigger;
    }

    public static PullRequestToCauseConverter toGitHubPRCause(GitHubPRRepository localRepo,
                                                              LoggingTaskListenerWrapper listener,
                                                              GitHubPRTrigger trigger) {
        return new PullRequestToCauseConverter(localRepo, listener, trigger);
    }

    @Override
    public GitHubPRCause apply(final GHPullRequest remotePR) {
        for (GitHubPREvent event: trigger.getEvents()) {
            final GitHubPRCause causeCheck = toCause(remotePR).apply(event);
            if (null != causeCheck) {
                // If this is a skip trigger return null to indicate that the PR should be skipped
                // (and don't continue to evaluate any more potential causes)
                return causeCheck.isSkip() ? null : causeCheck;
            }
        }
        return null;
    }

    @VisibleForTesting
    /* package */ EventToCauseConverter toCause(GHPullRequest remotePR) {
        return new EventToCauseConverter(remotePR);
    }

    @VisibleForTesting
    /* package */ class EventToCauseConverter implements Function<GitHubPREvent, GitHubPRCause> {
        private final GHPullRequest remotePR;

        EventToCauseConverter(GHPullRequest remotePR) {
            this.remotePR = remotePR;
        }

        @Override
        public GitHubPRCause apply(GitHubPREvent event) {
            //null if local not existed before
            @CheckForNull GitHubPRPullRequest localPR = localRepo.getPulls().get(remotePR.getNumber());
            try {
                return event.check(trigger, remotePR, localPR, listener);
            } catch (IOException e) {
                LOGGER.warn("Can't check trigger event", e);
                listener.error("Can't check trigger event, so skipping PR ({})", e.getMessage());
                return null;
            }
        }
    }
}
