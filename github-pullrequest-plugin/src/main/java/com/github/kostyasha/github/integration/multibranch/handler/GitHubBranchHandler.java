package com.github.kostyasha.github.integration.multibranch.handler;

import hudson.Extension;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchHandler  extends GitHubHandler {




    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends GitHubHandlerDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Branch Handler";
        }
    }
}
