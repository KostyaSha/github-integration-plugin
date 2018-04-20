package com.github.kostyasha.github.integration.tag;

import com.github.kostyasha.github.integration.generic.GitHubEnv;
import com.github.kostyasha.github.integration.generic.GitHubRepoEnv;
import hudson.model.ParameterValue;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Kanstantsin Shautsou
 */
public enum GitHubTagEnv implements GitHubEnv<GitHubTagCause> {
    NAME(GitHubTagCause::getTagName),
    SHORT_DESC(GitHubTagCause::getShortDescription),
    TITLE(GitHubTagCause::getTitle),
    URL((Function<GitHubTagCause, String>) c -> c.getHtmlUrl().toString()),
    HEAD_SHA(GitHubTagCause::getCommitSha),
    FULL_REF(GitHubTagCause::getFullRef),
    CAUSE_SKIP(GitHubTagCause::isSkip);

    public static final String PREFIX = "GITHUB_TAG_";

    private Function<GitHubTagCause, ParameterValue> fun;

    private GitHubTagEnv(Function<GitHubTagCause, String> fun) {
        this.fun = c -> param(fun.apply(c));
    }

    private GitHubTagEnv(Predicate<GitHubTagCause> fun) {
        this.fun = c -> param(fun.test(c));
    }

    @Override
    public void addParam(GitHubTagCause cause, List<ParameterValue> params) {
        params.add(fun.apply(cause));
    }

    @Override
    public String toString() {
        return PREFIX.concat(name());
    }

    public static void getParams(GitHubTagCause cause, List<ParameterValue> params) {
        GitHubEnv.getParams(GitHubTagEnv.class, cause, params);
        GitHubEnv.getParams(GitHubRepoEnv.class, cause, params);
    }
}
