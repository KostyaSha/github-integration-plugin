package com.github.kostyasha.github.integration.multibranch.handler;

import hudson.model.AbstractDescribableImpl;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubHandler extends AbstractDescribableImpl<GitHubHandler> {

    @Override
    public GitHubHandlerDescriptor getDescriptor() {
        return (GitHubHandlerDescriptor) super.getDescriptor();
    }

}
