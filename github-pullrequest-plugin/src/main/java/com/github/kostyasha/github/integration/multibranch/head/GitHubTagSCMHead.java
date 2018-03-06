package com.github.kostyasha.github.integration.multibranch.head;

import javax.annotation.Nonnull;

public class GitHubTagSCMHead extends GitHubSCMHead {
    public GitHubTagSCMHead(@Nonnull String name, String sourceId) {
        super(name, sourceId);
    }

    @Override
    public String getPronounPrefix() {
        return "Tag";
    }
}
