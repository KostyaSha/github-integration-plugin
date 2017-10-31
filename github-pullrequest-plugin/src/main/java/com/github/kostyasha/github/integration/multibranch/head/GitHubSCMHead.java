package com.github.kostyasha.github.integration.multibranch.head;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMHead;

import javax.annotation.Nonnull;

public abstract class GitHubSCMHead extends SCMHead {
    public GitHubSCMHead(@Nonnull String name) {
        super(name);
    }
}
