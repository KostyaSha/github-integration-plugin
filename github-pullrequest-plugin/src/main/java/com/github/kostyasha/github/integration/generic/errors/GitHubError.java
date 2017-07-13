package com.github.kostyasha.github.integration.generic.errors;

import hudson.ExtensionPoint;

import javax.annotation.Nonnull;

/**
 * Custom errors that participate in list of {@link GitHubErrorsAction}.
 * index.groovy
 *
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubError implements ExtensionPoint {
    private String description;

    public GitHubError(@Nonnull String description) {
        this.description = description;
    }

    @Nonnull
    public abstract String getTitle();

    /**
     * Whether to show error on job page. Useful when error visibility will be known after delay.
     */
    public boolean isVisible() {
        return true;
    }

    /**
     * Raw displayed html content as description.
     */
    @Nonnull
    public String getHtmlDescription() {
        return description;
    }
}
