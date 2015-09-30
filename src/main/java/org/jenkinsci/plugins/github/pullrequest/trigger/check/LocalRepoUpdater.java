package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.base.Function;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class LocalRepoUpdater implements Function<GHPullRequest, GHPullRequest> {
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
        switch (remotePR.getState()) {
            case OPEN:
                try {
                    localRepo.getPulls().put(remotePR.getNumber(), new GitHubPRPullRequest(remotePR));
                } catch (IOException e) {
                    LOGGER.warn("Can't store to local storage PR #{}", remotePR.getNumber(), e);
                }
                break;
            case CLOSED:
                localRepo.getPulls().remove(remotePR.getNumber()); // don't store
                break;
        }
        return remotePR;
    }
}

