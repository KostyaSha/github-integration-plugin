package com.github.kostyasha.github.integration.multibranch.head;

import jenkins.scm.api.SCMHead;

import javax.annotation.Nonnull;

import hudson.Extension;
import hudson.util.AlternativeUiTextProvider;

public abstract class GitHubSCMHead extends SCMHead {
    private final String sourceId;

    public GitHubSCMHead(@Nonnull String name, String sourceId) {
        super(name);
        this.sourceId = sourceId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getPronounPrefix() {
        return null;
    }

    @Extension
    public static class Pronoun extends AlternativeUiTextProvider {
        @Override
        public <T> String getText(Message<T> text, T context) {
            if (text == SCMHead.PRONOUN && context instanceof GitHubSCMHead) {
                GitHubSCMHead head = (GitHubSCMHead) context;
                String prefix = head.getPronounPrefix();
                if (prefix == null) {
                    return head.getName();
                }
                return prefix + " " + head.getName();
            }
            return null;
        }
    }
}
