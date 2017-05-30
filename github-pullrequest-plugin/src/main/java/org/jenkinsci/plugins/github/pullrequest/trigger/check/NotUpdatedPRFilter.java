package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.base.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class NotUpdatedPRFilter implements Predicate<GHPullRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotUpdatedPRFilter.class);

    private final GitHubPRRepository localRepo;
    private final LoggingTaskListenerWrapper logger;

    private NotUpdatedPRFilter(GitHubPRRepository localRepo, LoggingTaskListenerWrapper logger) {
        this.localRepo = localRepo;
        this.logger = logger;
    }

    public static NotUpdatedPRFilter notUpdated(GitHubPRRepository localRepo, LoggingTaskListenerWrapper logger) {
        return new NotUpdatedPRFilter(localRepo, logger);
    }

    @Override
    public boolean apply(GHPullRequest remotePR) {
        @CheckForNull GitHubPRPullRequest localPR = localRepo.getPulls().get(remotePR.getNumber());

        if (!isUpdated(remotePR, localPR)) { // light check
            logger.debug("PR [#{} {}] not changed", remotePR.getNumber(), remotePR.getTitle());
            return false;
        }
        logger.debug("PR [#{} {}] was changed", remotePR.getNumber(), remotePR.getTitle());

        return true;
    }

    /**
     * lightweight check that comments and time were changed
     */
    private static boolean isUpdated(GHPullRequest remotePR, GitHubPRPullRequest localPR) {
        final int prNum = remotePR.getNumber();
        if (isNull(localPR)) {
            LOGGER.trace("Local PR is null for remote pr #{}", prNum);
            return true; // we don't know yet
        }
        try {

            boolean prUpd = new CompareToBuilder()
                    .append(localPR.getPrUpdatedAt(), remotePR.getUpdatedAt()).build() < 0; // by time
            LOGGER.trace("#{}, updatedAt {}, localPR:{}, remotePR:{}",
                    prNum, prUpd, localPR.getPrUpdatedAt(), remotePR.getUpdatedAt());

            boolean issueUpd = new CompareToBuilder()
                    .append(localPR.getIssueUpdatedAt(), remotePR.getIssueUpdatedAt()).build() < 0;
            LOGGER.trace("#{}, updatedAt {}, issue:{}, issue:{}",
                    prNum, issueUpd, localPR.getIssueUpdatedAt(), remotePR.getIssueUpdatedAt());

            boolean headUpd = !StringUtils.equals(localPR.getHeadSha(), remotePR.getHead().getSha()); // or head?
            LOGGER.trace("#{}, hash {}, issue:{}, issue:{}",
                    prNum, headUpd, localPR.getHeadSha(), remotePR.getHead().getSha());

            boolean updated = prUpd || issueUpd || headUpd;

            if (updated) {
                LOGGER.info("Pull request #{} was created by {}, last prUpdated: {}, issue updated: {}",
                        localPR.getNumber(), localPR.getUserLogin(), localPR.getPrUpdatedAt(), localPR.getIssueUpdatedAt());
            }

            return updated;
        } catch (IOException e) {
            // should never happen because
            LOGGER.warn("Can't compare PR [#{} {}] with local copy for update",
                    remotePR.getNumber(), remotePR.getTitle(), e);
            return false;
        }
    }
}
