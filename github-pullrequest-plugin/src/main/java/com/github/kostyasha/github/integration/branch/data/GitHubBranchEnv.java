package com.github.kostyasha.github.integration.branch.data;

import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.generic.GitHubEnv;
import com.github.kostyasha.github.integration.generic.GitHubRepoEnv;
import hudson.model.ParameterValue;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Kanstantsin Shautsou
 */
public enum GitHubBranchEnv implements GitHubEnv<GitHubBranchCause> {
    NAME(GitHubBranchCause::getBranchName),
    SHORT_DESC(GitHubBranchCause::getShortDescription),
    TITLE(GitHubBranchCause::getTitle),
    URL((Function<GitHubBranchCause, String>) c -> c.getHtmlUrl().toString()),
    HEAD_SHA(GitHubBranchCause::getCommitSha),
    FULL_REF(GitHubBranchCause::getFullRef),
    CAUSE_SKIP(GitHubBranchCause::isSkip);

    public static final String PREFIX = "GITHUB_BRANCH_";

    private Function<GitHubBranchCause, ParameterValue> fun;

    GitHubBranchEnv(Function<GitHubBranchCause, String> fun) {
        this.fun = c -> param(fun.apply(c));
    }

    GitHubBranchEnv(Predicate<GitHubBranchCause> fun) {
        this.fun = c -> param(fun.test(c));
    }

    @Override
    public void addParam(GitHubBranchCause cause, List<ParameterValue> params) {
        params.add(fun.apply(cause));
    }

    @Override
    public String toString() {
        return PREFIX.concat(name());
    }

    public static void getParams(GitHubBranchCause cause, List<ParameterValue> params) {
        GitHubEnv.getParams(GitHubBranchEnv.class, cause, params);
        GitHubEnv.getParams(GitHubRepoEnv.class, cause, params);
    }
}
