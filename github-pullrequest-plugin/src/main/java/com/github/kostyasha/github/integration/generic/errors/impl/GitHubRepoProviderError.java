package com.github.kostyasha.github.integration.generic.errors.impl;

import com.github.kostyasha.github.integration.generic.errors.GitHubError;

import javax.annotation.Nonnull;

/**
 * Used during {@link com.github.kostyasha.github.integration.generic.GitHubRepoProvider} resolve.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubRepoProviderError extends GitHubError {

    public GitHubRepoProviderError(@Nonnull String description) {
        super(description);
    }

    @Nonnull
    @Override
    public String getTitle() {
        return "GitHub Repo Provider Error";
    }
}
