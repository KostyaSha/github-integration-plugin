package com.github.kostyasha.github.integration.multibranch.head;

import com.github.kostyasha.github.integration.branch.GitHubBranchCause;

import javax.annotation.Nonnull;

public class GitHubBranchSCMHead extends GitHubSCMHead {
    public GitHubBranchSCMHead(@Nonnull String name, GitHubBranchCause branchCause) {
        super(name, branchCause);

    }
}
