package com.github.kostyasha.github.integration.multibranch.head;

import jenkins.scm.api.SCMHead;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.kohsuke.github.GHRepository;

public abstract class GitHubSCMHead extends SCMHead {
    private static final long serialVersionUID = 1L;

    private final String sourceId;

    public GitHubSCMHead(@Nonnull String name, String sourceId) {
        super(name);
        this.sourceId = sourceId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public abstract String getHeadSha(GHRepository remoteRepo) throws IOException;
}
