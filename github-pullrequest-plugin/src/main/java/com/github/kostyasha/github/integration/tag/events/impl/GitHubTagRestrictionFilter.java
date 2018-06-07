package com.github.kostyasha.github.integration.tag.events.impl;

import com.github.kostyasha.github.integration.generic.GitHubTagDecisionContext;
import com.github.kostyasha.github.integration.tag.GitHubTagCause;
import com.github.kostyasha.github.integration.tag.events.GitHubTagEvent;
import com.github.kostyasha.github.integration.tag.events.GitHubTagEventDescriptor;
import hudson.Extension;
import org.jenkinsci.Symbol;
import org.kohsuke.github.GHTag;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GitHubTagRestrictionFilter extends GitHubTagEvent {

    private static final String DISPLAY_NAME = "Tag Restrictions";

    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubTagRestrictionFilter.class);

    private boolean exclude;

    private boolean matchAsPattern;
    private Set<String> matchCriteria;

    @DataBoundConstructor
    public GitHubTagRestrictionFilter() {
    }

    public String getMatchCriteriaStr() {
        return String.join(LINE_SEPARATOR, matchCriteria);
    }

    @DataBoundSetter
    public void setMatchCriteriaStr(String matchCriteria) {
        this.matchCriteria = Stream.of(matchCriteria
                .split(LINE_SEPARATOR))
                .collect(Collectors.toSet());
    }

    public boolean isExclude() {
        return exclude;
    }

    @DataBoundSetter
    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }

    public boolean isMatchAsPattern() {
        return matchAsPattern;
    }

    @DataBoundSetter
    public void setMatchAsPattern(boolean matchAsPattern) {
        this.matchAsPattern = matchAsPattern;
    }

    // visible for testing
    public Set<String> getMatchCriteria() {
        return matchCriteria;
    }

    @Override
    public GitHubTagCause check(@Nonnull GitHubTagDecisionContext context) throws IOException {
        GHTag remoteTag = context.getRemoteTag();

        String name = remoteTag.getName();
        if (matchCriteria.isEmpty() || tagIsAllowed(name)) {
            if (matchCriteria.isEmpty()) {
                LOGGER.warn("Tag restriction filter added but no match criteria set, all tags allowed");
            }
            // filter allows checking other events
            return null;
        }

        return toCause(context, true, "Tag [%s] filtered by tag name restriction filter", name);
    }

    private boolean tagIsAllowed(String name) {
        for (String pattern : matchCriteria) {
            LOGGER.trace("Checking tag [{}] against pattern [{}] - exclude [{}]", name, pattern, exclude);
            if (matches(name, pattern)) {
                if (exclude) {
                    LOGGER.debug("Tag [{}] matches pattern [{}], will be excluded", name, pattern);
                    return false;
                }
                LOGGER.debug("Tag [%s] matched pattern [{}] and is marked for inclusion", name, pattern);
                return true;
            }
        }
        LOGGER.trace("Tag [{}] matched no patterns, included [{}]", name, exclude);
        return exclude;
    }

    private boolean matches(String name, String pattern) {
        if (!matchAsPattern) {
            LOGGER.debug("Checking Tag [{}] against exact match [{}]", name, pattern);
            return name.equals(pattern);
        }

        try {
            LOGGER.debug("Checking tag [{}] against pattern [{}].", name, pattern);
            return Pattern.compile(pattern).matcher(name).matches();
        } catch (PatternSyntaxException e) {
            LOGGER.error("Invalid pattern [{}] detected checking tag [{}]", pattern, name, e);
            return false;
        }
    }

    private GitHubTagCause toCause(GitHubTagDecisionContext context, boolean skip,
                                   String message, Object... args) {
        return context.newCause(String.format(message, args), skip);
    }

    @Symbol("restrictions")
    @Extension
    public static class Descriptor extends GitHubTagEventDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }

}
