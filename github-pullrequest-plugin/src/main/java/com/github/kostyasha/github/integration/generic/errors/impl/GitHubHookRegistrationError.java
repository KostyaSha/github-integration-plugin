package com.github.kostyasha.github.integration.generic.errors.impl;

import com.github.kostyasha.github.integration.generic.errors.GitHubError;

import javax.annotation.Nonnull;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubHookRegistrationError extends GitHubError {
    public GitHubHookRegistrationError(@Nonnull String description) {
        super(description);
    }

    @Nonnull
    @Override
    public String getTitle() {
        return "GitHub Hook Registration error";
    }

}
