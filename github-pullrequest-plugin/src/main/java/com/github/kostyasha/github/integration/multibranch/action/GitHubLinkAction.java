package com.github.kostyasha.github.integration.multibranch.action;

import hudson.Functions;
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
        return Functions.getResourcePath() + "/plugin/github-pullrequest/git-branch.svg";
    }

    @Override
    public String getUrlName() {
        return url;
    }

}
