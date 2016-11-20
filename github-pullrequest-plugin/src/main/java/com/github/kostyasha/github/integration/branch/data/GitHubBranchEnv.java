package com.github.kostyasha.github.integration.branch.data;

import hudson.model.BooleanParameterValue;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;

import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;

/**
 * @author Kanstantsin Shautsou
 */
public enum GitHubBranchEnv {
    NAME,
    SHORT_DESC,
    TITLE,
    URL,
    HEAD_SHA,
    FULL_REF,
    CAUSE_SKIP,
    STATE;

    public static final String PREFIX = "GITHUB_BRANCH_";

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
