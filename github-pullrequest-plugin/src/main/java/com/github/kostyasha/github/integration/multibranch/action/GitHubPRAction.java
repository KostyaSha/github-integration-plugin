package com.github.kostyasha.github.integration.multibranch.action;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.kohsuke.github.GHRepository;

/**
 * Add a link to repository pr
 *
 * @author Anton Tanasenko
 */
public class GitHubPRAction extends GitHubLinkAction {

    private Integer pr;

    public GitHubPRAction(GHRepository remoteRepository, Integer pr) {
        super(buildUrl(remoteRepository, pr));
        this.pr = pr;
    }

    @Override
    public String getIconFileName() {
        return GitHubPRPullRequest.getIconFileName();
    }

    @Override
    public String getDisplayName() {
        return "PR#" + pr;
    }

    private static String buildUrl(GHRepository remoteRepository, Integer pr) {
        return remoteRepository.getHtmlUrl().toExternalForm() + "/pull/" + pr;
    }

}
