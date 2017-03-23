package org.jenkinsci.plugins.github.pullrequest.data;

import hudson.model.BooleanParameterValue;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;

import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;

/**
 * @author lanwen (Merkushev Kirill)
 */
public enum GitHubPREnv {
    TRIGGER_SENDER_AUTHOR,
    TRIGGER_SENDER_EMAIL,
    COMMIT_AUTHOR_NAME,
    COMMIT_AUTHOR_EMAIL,
    TARGET_BRANCH,
    SOURCE_BRANCH,
    AUTHOR_EMAIL,
    BODY,
    SHORT_DESC,
    TITLE,
    URL,
    SOURCE_REPO_OWNER,
    HEAD_SHA,
    COND_REF,
    CAUSE_SKIP,
    NUMBER,
    STATE,
    COMMENT_BODY,
    COMMENT_BODY_MATCH,
    LABELS;

    public static final String PREFIX = "GITHUB_PR_";

    public ParameterValue param(String value) {
        return new StringParameterValue(toString(), escapeJava(trimToEmpty(value)));
    }

    public ParameterValue param(boolean value) {
        return new BooleanParameterValue(toString(), value);
    }

    @Override
    public String toString() {
        return PREFIX.concat(name());
    }
}
