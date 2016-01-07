package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.base.Function;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.kohsuke.github.GHPullRequest;

import javax.annotation.CheckForNull;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class UserRestrictionPopulator implements Function<GHPullRequest, GHPullRequest> {
    private final GitHubPRRepository localRepo;
    private final GitHubPRTrigger trigger;

    private UserRestrictionPopulator(GitHubPRRepository localRepo, GitHubPRTrigger trigger) {
        this.localRepo = localRepo;
        this.trigger = trigger;
    }

    public static UserRestrictionPopulator prepareUserRestrictionFilter(
            GitHubPRRepository localRepo, GitHubPRTrigger trigger) {
        return new UserRestrictionPopulator(localRepo, trigger);
    }

    @Override
    public GHPullRequest apply(GHPullRequest remotePR) {
        if (trigger.getUserRestriction() != null) {
            @CheckForNull GitHubPRPullRequest localPR = localRepo.getPulls().get(remotePR.getNumber());
            trigger.getUserRestriction().populate(remotePR, localPR, trigger);
        }
        return remotePR;
    }
}
