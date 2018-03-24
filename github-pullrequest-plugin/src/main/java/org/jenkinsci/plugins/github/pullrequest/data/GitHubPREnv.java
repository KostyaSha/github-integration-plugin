package org.jenkinsci.plugins.github.pullrequest.data;

import hudson.model.ParameterValue;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;

import com.github.kostyasha.github.integration.generic.GitHubEnv;
import com.github.kostyasha.github.integration.generic.GitHubRepoEnv;

/**
 * @author lanwen (Merkushev Kirill)
 */
public enum GitHubPREnv implements GitHubEnv<GitHubPRCause> {
    TRIGGER_SENDER_AUTHOR(GitHubPRCause::getTriggerSenderName),
    TRIGGER_SENDER_EMAIL(GitHubPRCause::getTriggerSenderEmail),
    COMMIT_AUTHOR_NAME(GitHubPRCause::getCommitAuthorName),
    COMMIT_AUTHOR_EMAIL(GitHubPRCause::getCommitAuthorEmail),
    TARGET_BRANCH(GitHubPRCause::getTargetBranch),
    SOURCE_BRANCH(GitHubPRCause::getSourceBranch),
    AUTHOR_EMAIL(GitHubPRCause::getPrAuthorEmail),
    BODY(GitHubPRCause::getBody),
    SHORT_DESC(GitHubPRCause::getShortDescription),
    TITLE(GitHubPRCause::getTitle),
    URL((Function<GitHubPRCause, String>) c -> c.getHtmlUrl().toString()),
    SOURCE_REPO_OWNER(GitHubPRCause::getSourceRepoOwner),
    HEAD_SHA(GitHubPRCause::getHeadSha),
    COND_REF(GitHubPRCause::getCondRef),
    CAUSE_SKIP(GitHubPRCause::isSkip),
    NUMBER((Function<GitHubPRCause, String>) c -> String.valueOf(c.getNumber())),
    STATE(GitHubPRCause::getState),
    COMMENT_BODY(GitHubPRCause::getCommentBody),
    COMMENT_BODY_MATCH(GitHubPRCause::getCommentBodyMatch),
    LABELS((Function<GitHubPRCause, String>) c -> String.join(",", c.getLabels()));

    public static final String PREFIX = "GITHUB_PR_";

    private Function<GitHubPRCause, ParameterValue> fun;

    private GitHubPREnv(Function<GitHubPRCause, String> fun) {
        this.fun = c -> param(fun.apply(c));
    }

    private GitHubPREnv(Predicate<GitHubPRCause> fun) {
        this.fun = c -> param(fun.test(c));
    }

    @Override
    public void addParam(GitHubPRCause cause, List<ParameterValue> params) {
        params.add(fun.apply(cause));
    }

    @Override
    public String toString() {
        return PREFIX.concat(name());
    }

    public static void getParams(GitHubPRCause cause, List<ParameterValue> params) {
        GitHubEnv.getParams(GitHubPREnv.class, cause, params);
        GitHubEnv.getParams(GitHubRepoEnv.class, cause, params);
    }
}
