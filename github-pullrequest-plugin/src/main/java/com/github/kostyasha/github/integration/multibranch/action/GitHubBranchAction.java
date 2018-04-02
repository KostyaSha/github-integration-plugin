package com.github.kostyasha.github.integration.multibranch.action;

import org.kohsuke.github.GHRepository;

/**
 * Add a link to repository branch
 * 
 * @author Anton Tanasenko
 */
public class GitHubBranchAction extends GitHubLinkAction {

    private String branch;

    public GitHubBranchAction(GHRepository remoteRepository, String branch) {
        super(buildUrl(remoteRepository, branch));
        this.branch = branch;
    }

    @Override
    public String getDisplayName() {
        return "Branch " + branch;
    }

    private static String buildUrl(GHRepository remoteRepository, String branch) {
        String repoUrl = remoteRepository.getHtmlUrl().toExternalForm();
        if (remoteRepository.getDefaultBranch().equals(branch)) {
            return repoUrl;
        }
        return repoUrl + "/tree/" + branch;
    }

}
