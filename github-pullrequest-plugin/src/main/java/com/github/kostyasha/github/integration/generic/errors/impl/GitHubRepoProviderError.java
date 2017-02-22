package com.github.kostyasha.github.integration.generic.errors.impl;

import com.github.kostyasha.github.integration.generic.errors.GitHubError;

import javax.annotation.Nonnull;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubRepoProviderError extends GitHubError {

    private Throwable error;
    private String description;

    public GitHubRepoProviderError(@Nonnull Throwable error) {
        this.error = error;
    }

    public GitHubRepoProviderError(String description) {
        this.description = description;
    }

    public Throwable getError() {
        return error;
    }

    @Override
    public String getTitle() {
        return "GitHub Repo Provider Error";
    }

    /**
     * Raw data shown on error page.
     */
    public String getHtmlDescription() {
        return description;
    }
}
