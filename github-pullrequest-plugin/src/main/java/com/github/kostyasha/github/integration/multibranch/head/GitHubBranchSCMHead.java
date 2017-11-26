package com.github.kostyasha.github.integration.multibranch.head;

import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.generic.GitHubCause;

import javax.annotation.Nonnull;

public class GitHubBranchSCMHead extends GitHubSCMHead {
    public GitHubBranchSCMHead(GitHubBranchCause branchCause) {
        super(branchCause.getBranchName(), branchCause);
    }

    public GitHubBranchSCMHead(@Nonnull String name) {
        super(name, null);
    }
}
