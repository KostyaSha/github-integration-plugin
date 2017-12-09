package com.github.kostyasha.github.integration.multibranch.head;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;

import javax.annotation.Nonnull;

public class GitHubPRSCMHead extends GitHubSCMHead {

    public GitHubPRSCMHead(@Nonnull GitHubPRCause prCause) {
        super(Integer.toString(prCause.getNumber()));
    }

    public GitHubPRSCMHead(@Nonnull String name) {
        super(name);
    }
}
