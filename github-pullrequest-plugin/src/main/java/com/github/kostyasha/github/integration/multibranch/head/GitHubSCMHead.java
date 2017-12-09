package com.github.kostyasha.github.integration.multibranch.head;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import jenkins.scm.api.SCMHead;

import javax.annotation.Nonnull;

public abstract class GitHubSCMHead extends SCMHead {
    private final String sourceId;

    public GitHubSCMHead(@Nonnull String name, String sourceId) {
        super(name);
        this.sourceId = sourceId;
    }

    public String getSourceId() {
        return sourceId;
    }
}
