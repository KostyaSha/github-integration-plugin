package com.github.kostyasha.github.integration.multibranch.handler;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import hudson.DescriptorExtensionList;
import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import org.kohsuke.github.GHRepository;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubHandler extends AbstractDescribableImpl<GitHubHandler> {

    public abstract void handle(@Nonnull SCMHeadObserver observer,
                                @CheckForNull SCMHeadEvent headEvent,
                                @Nonnull GitHubRepo localRepo,
                                @Nonnull GHRepository remoteRepo,
                                @Nonnull TaskListener taskListener,
                                @Nonnull GitHubSCMSource source
    ) throws IOException;
}
