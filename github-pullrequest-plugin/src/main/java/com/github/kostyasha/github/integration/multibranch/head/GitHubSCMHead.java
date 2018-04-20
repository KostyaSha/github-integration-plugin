package com.github.kostyasha.github.integration.multibranch.head;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
import jenkins.scm.api.SCMHead;
import org.kohsuke.github.GHRepository;

import javax.annotation.Nonnull;
import java.io.IOException;

public abstract class GitHubSCMHead<T extends GitHubCause<T>> extends SCMHead {
    private static final long serialVersionUID = 1L;

    private final String sourceId;

    public GitHubSCMHead(@Nonnull String name, String sourceId) {
        super(name);
        this.sourceId = sourceId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public abstract String fetchHeadSha(GHRepository remoteRepo) throws IOException;

    public abstract String getHeadSha(T cause);

    public GitHubSCMRevision createSCMRevision(T cause) {
        return new GitHubSCMRevision(this, getHeadSha(cause), cause);
    }
}
