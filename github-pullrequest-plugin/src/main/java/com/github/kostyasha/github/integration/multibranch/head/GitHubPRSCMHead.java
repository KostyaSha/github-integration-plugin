package com.github.kostyasha.github.integration.multibranch.head;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;

import javax.annotation.Nonnull;

public class GitHubPRSCMHead extends GitHubSCMHead {

    public GitHubPRSCMHead(@Nonnull String name, @Nonnull GitHubPRCause prCause) {
        super(name, prCause);
    }

}
