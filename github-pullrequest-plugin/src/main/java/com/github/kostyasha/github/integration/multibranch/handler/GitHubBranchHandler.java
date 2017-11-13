package com.github.kostyasha.github.integration.multibranch.handler;

import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.generic.GitHubCause;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchHandler  extends GitHubHandler {

    private List<GitHubBranchEvent> events = new ArrayList<>();

    @DataBoundConstructor
    public GitHubBranchHandler() {
    }

    public List<GitHubBranchEvent> getEvents() {
        return events;
    }

    @DataBoundSetter
    public GitHubBranchHandler setEvents(List<GitHubBranchEvent> events) {
        this.events = events;
        return this;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Nonnull
    @Override
    public List<GitHubCause> handle(@Nonnull GitHubRepo localRepo, @Nonnull GHRepository remoteRepo,
                                    @Nonnull TaskListener taskListener) {
        GitHubBranchRepository localBranches = localRepo.getBranchRepository();

        return Collections.emptyList();
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
