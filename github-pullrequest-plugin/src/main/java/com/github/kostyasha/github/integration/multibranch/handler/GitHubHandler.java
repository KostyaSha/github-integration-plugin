package com.github.kostyasha.github.integration.multibranch.handler;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import hudson.DescriptorExtensionList;
import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.kohsuke.github.GHRepository;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubHandler extends AbstractDescribableImpl<GitHubHandler> {

    @Override
    public GitHubHandlerDescriptor getDescriptor() {
        return (GitHubHandlerDescriptor) super.getDescriptor();
    }

    @Nonnull
    public abstract List<GitHubCause> handle(@Nonnull GitHubRepo localRepo,
                                             @Nonnull GHRepository remoteRepo,
                                             @Nonnull TaskListener taskListener);
}
