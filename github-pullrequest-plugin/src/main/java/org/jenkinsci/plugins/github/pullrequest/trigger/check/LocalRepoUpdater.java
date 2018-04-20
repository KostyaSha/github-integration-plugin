package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.base.Function;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kohsuke.github.GHIssueState.CLOSED;
import static org.kohsuke.github.GHIssueState.OPEN;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class LocalRepoUpdater implements Function<GHPullRequest, GHPullRequest>, java.util.function.Function<GHPullRequest, GHPullRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRepoUpdater.class);
    private final GitHubPRRepository localRepo;

    private LocalRepoUpdater(GitHubPRRepository localRepo) {
        this.localRepo = localRepo;
    }

    public static LocalRepoUpdater updateLocalRepo(GitHubPRRepository localRepo) {
        return new LocalRepoUpdater(localRepo);
    }

    @Override
    public GHPullRequest apply(GHPullRequest remotePR) {
        if (remotePR.getState() == OPEN) {
            localRepo.getPulls().put(remotePR.getNumber(), new GitHubPRPullRequest(remotePR));
        } else if (remotePR.getState() == CLOSED) {
            localRepo.getPulls().remove(remotePR.getNumber()); // don't store
        }

        return remotePR;
    }
}

