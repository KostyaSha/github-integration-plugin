package com.github.kostyasha.github.integration.generic.errors.impl;

import com.github.kostyasha.github.integration.generic.errors.GitHubError;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Used during {@link com.github.kostyasha.github.integration.generic.GitHubRepoProvider} resolve.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubRepoProviderError extends GitHubError {

    public GitHubRepoProviderError(@NonNull String description) {
        super(description);
    }

    @NonNull
    @Override
    public String getTitle() {
        return "GitHub Repo Provider Error";
    }
}
