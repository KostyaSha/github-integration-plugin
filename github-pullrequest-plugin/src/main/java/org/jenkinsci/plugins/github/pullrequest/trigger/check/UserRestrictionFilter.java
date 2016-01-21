package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHPullRequest;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class UserRestrictionFilter implements Predicate<GHPullRequest> {
    private final LoggingTaskListenerWrapper listener;
    private final GitHubPRUserRestriction userRestriction;

    private UserRestrictionFilter(LoggingTaskListenerWrapper listener, GitHubPRUserRestriction userRestriction) {
        this.listener = listener;
        this.userRestriction = userRestriction;
    }

    public static Predicate<GHPullRequest> withUserRestriction(LoggingTaskListenerWrapper listener,
                                                               GitHubPRUserRestriction userRestriction) {
        if (isNull(userRestriction)) {
            return Predicates.alwaysTrue();
        } else {
            return new UserRestrictionFilter(listener, userRestriction);
        }
    }

    @Override
    public boolean apply(GHPullRequest remotePR) {
        if (!userRestriction.isWhitelisted(remotePR.getUser())) {
            listener.info("Skipping [#{} {}] because of user restriction (user - {})",
                    remotePR.getNumber(), remotePR.getTitle(), remotePR.getUser());
            return false;
        }

        return true;
    }
}
