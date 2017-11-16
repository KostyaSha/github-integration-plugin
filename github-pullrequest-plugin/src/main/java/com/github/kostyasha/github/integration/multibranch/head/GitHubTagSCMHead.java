package com.github.kostyasha.github.integration.multibranch.head;

import com.github.kostyasha.github.integration.tag.GitHubTagCause;

import javax.annotation.Nonnull;

public class GitHubTagSCMHead extends GitHubSCMHead {
    public GitHubTagSCMHead(@Nonnull String name, GitHubTagCause cause) {
        super(name, cause);
    }
}
