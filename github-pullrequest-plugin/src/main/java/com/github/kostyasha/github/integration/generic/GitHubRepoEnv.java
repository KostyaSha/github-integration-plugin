package com.github.kostyasha.github.integration.generic;

import hudson.model.ParameterValue;

import java.util.List;
import java.util.function.Function;

/**
 * @author Kanstantsin Shautsou
 */
public enum GitHubRepoEnv implements GitHubEnv<GitHubCause<?>> {
    GIT_URL(GitHubCause::getGitUrl),
    SSH_URL(GitHubCause::getSshUrl);

    public static final String PREFIX = "GITHUB_REPO_";

    private Function<GitHubCause<?>, ParameterValue> fun;

    GitHubRepoEnv(Function<GitHubCause<?>, String> fun) {
        this.fun = c -> param(fun.apply(c));
    }

    @Override
    public void addParam(GitHubCause<?> cause, List<ParameterValue> params) {
        params.add(fun.apply(cause));
    }

    @Override
    public String toString() {
        return PREFIX.concat(name());
    }

}
