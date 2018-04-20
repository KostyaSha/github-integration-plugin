package com.github.kostyasha.github.integration.generic;

import hudson.model.BooleanParameterValue;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;

import java.util.List;

import static org.apache.commons.lang.StringEscapeUtils.escapeJava;
import static org.apache.commons.lang.StringUtils.trimToEmpty;

public interface GitHubEnv<T extends GitHubCause<?>> {

    void addParam(T cause, List<ParameterValue> params);

    default ParameterValue param(String value) {
        return new StringParameterValue(toString(), escapeJava(trimToEmpty(value)));
    }

    default ParameterValue param(boolean value) {
        return new BooleanParameterValue(toString(), value);
    }

    public static <T extends GitHubCause<?>, X extends Enum<X> & GitHubEnv<T>> void getParams(Class<X> enumClass, T cause, List<ParameterValue> params) {
        for (GitHubEnv<T> env : enumClass.getEnumConstants()) {
            env.addParam(cause, params);
        }
    }
}
