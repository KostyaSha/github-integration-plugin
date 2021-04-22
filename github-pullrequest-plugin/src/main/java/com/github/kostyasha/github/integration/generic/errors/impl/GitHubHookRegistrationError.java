package com.github.kostyasha.github.integration.generic.errors.impl;

import com.github.kostyasha.github.integration.generic.errors.GitHubError;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * When hook registration in trigger fails.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubHookRegistrationError extends GitHubError {
    public GitHubHookRegistrationError(@NonNull String description) {
        super(description);
    }

    @NonNull
    @Override
    public String getTitle() {
        return "GitHub Hook Registration error";
    }

}
