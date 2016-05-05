package org.jenkinsci.plugins.github_integration.branch.trigger.check;

import com.google.common.base.Function;
import org.jenkinsci.plugins.github_integration.branch.GitHubLocalBranch;
import org.jenkinsci.plugins.github_integration.branch.GitHubBranchRepository;
import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kanstantsin Shautsou
 */
public class LocalRepoUpdater implements Function<GHBranch, GHBranch> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRepoUpdater.class);
    private final GitHubBranchRepository localRepo;

    private LocalRepoUpdater(GitHubBranchRepository localRepo) {
        this.localRepo = localRepo;
    }

    public static LocalRepoUpdater updateLocalRepo(GitHubBranchRepository localRepo) {
        return new LocalRepoUpdater(localRepo);
    }

    @Override
    public GHBranch apply(GHBranch remoteBranch) {
        localRepo.getBranches().put(remoteBranch.getName(), new GitHubLocalBranch(remoteBranch));

        return remoteBranch;
    }
}
