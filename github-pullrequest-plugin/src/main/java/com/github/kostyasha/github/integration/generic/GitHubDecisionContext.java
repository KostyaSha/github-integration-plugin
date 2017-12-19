package com.github.kostyasha.github.integration.generic;

import hudson.model.Item;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubDecisionContext {
    private final TaskListener listener;

    protected GitHubDecisionContext(@Nonnull TaskListener listener) {
        this.listener = listener;
    }

    @Nonnull
    public TaskListener getListener() {
        return listener;
    }
}
