package com.github.kostyasha.github.integration.multibranch.head;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import jenkins.scm.api.SCMHead;

import javax.annotation.Nonnull;

public abstract class GitHubSCMHead extends SCMHead {
    protected final GitHubCause cause;

    public GitHubSCMHead(@Nonnull String name, GitHubCause cause) {
        super(name);
        this.cause = cause;
    }

    public GitHubCause getCause() {
        return cause;
    }
}
