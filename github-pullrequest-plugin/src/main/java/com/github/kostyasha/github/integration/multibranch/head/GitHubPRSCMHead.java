package com.github.kostyasha.github.integration.multibranch.head;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;

import javax.annotation.Nonnull;

public class GitHubPRSCMHead extends GitHubSCMHead {

    public GitHubPRSCMHead(@Nonnull GitHubPRCause prCause, String sourceId) {
        super(Integer.toString(prCause.getNumber()), sourceId);
    }

    public GitHubPRSCMHead(@Nonnull String name, String sourceId) {
        super(name, sourceId);
    }

    @Override
    public String getPronounPrefix() {
        return "PR";
    }
}
