package com.github.kostyasha.github.integration.multibranch.handler;

import java.io.IOException;

import javax.annotation.Nonnull;

import hudson.model.AbstractDescribableImpl;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubHandler extends AbstractDescribableImpl<GitHubHandler> {

    public abstract void handle(@Nonnull GitHubSourceContext context) throws IOException;
}
