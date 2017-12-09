package com.github.kostyasha.github.integration.multibranch.head;


import javax.annotation.Nonnull;

public class GitHubBranchSCMHead extends GitHubSCMHead {
    public GitHubBranchSCMHead(@Nonnull String name, String sourceId) {
        super(name, sourceId);
    }
}
