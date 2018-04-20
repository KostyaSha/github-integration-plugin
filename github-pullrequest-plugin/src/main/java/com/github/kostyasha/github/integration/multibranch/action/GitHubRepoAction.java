package com.github.kostyasha.github.integration.multibranch.action;

import org.kohsuke.github.GHRepository;

/**
 * Add a link to repository
 *
 * @author Anton Tanasenko
 */
public class GitHubRepoAction extends GitHubLinkAction {

    private final String name;

    public GitHubRepoAction(GHRepository remoteRepository) {
        super(remoteRepository.getHtmlUrl().toExternalForm());
        this.name = remoteRepository.getFullName();
    }

    @Override
    public String getDisplayName() {
        return "Repo " + name;
    }

}
