package com.github.kostyasha.github.integration.multibranch.head;

import javax.annotation.Nonnull;

public class GitHubTagSCMHead extends GitHubSCMHead {
    private static final long serialVersionUID = 1L;

    public GitHubTagSCMHead(@Nonnull String name, String sourceId) {
        super(name, sourceId);
    }

    @Override
    public String getPronoun() {
        return "Tag " + getName();
    }
}
