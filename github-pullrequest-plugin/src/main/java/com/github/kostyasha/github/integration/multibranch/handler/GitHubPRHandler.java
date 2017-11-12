package com.github.kostyasha.github.integration.multibranch.handler;

import hudson.Extension;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPRHandler extends GitHubHandler {
    @CheckForNull
    private List<GitHubPREvent> events = new ArrayList<>();

    @DataBoundConstructor
    public GitHubPRHandler() {
    }

    public List<GitHubPREvent> getEvents() {
        return events;
    }

    @DataBoundSetter
    public GitHubPRHandler setEvents(List<GitHubPREvent> events) {
        this.events = events;
        return this;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends GitHubHandlerDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "GitHub PR Handler";
        }
    }
}
