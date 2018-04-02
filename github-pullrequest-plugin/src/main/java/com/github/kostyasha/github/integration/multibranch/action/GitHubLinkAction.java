package com.github.kostyasha.github.integration.multibranch.action;

import com.github.kostyasha.github.integration.branch.GitHubBranch;

import hudson.model.Action;

/**
 * Add a simple link to item
 * 
 * @author Anton Tanasenko
 */
public abstract class GitHubLinkAction implements Action {

    private final String url;

    protected GitHubLinkAction(String url) {
        this.url = url;
    }

    @Override
    public String getIconFileName() {
        return GitHubBranch.getIconFileName();
    }

    @Override
    public String getUrlName() {
        return url;
    }

}
