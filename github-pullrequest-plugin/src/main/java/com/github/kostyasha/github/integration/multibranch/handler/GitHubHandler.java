package com.github.kostyasha.github.integration.multibranch.handler;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.kohsuke.github.GHRepository;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.SCMHeadConsumer;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;

import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubHandler extends AbstractDescribableImpl<GitHubHandler> {

    public abstract void handle(@Nonnull SCMHeadConsumer consumer,
                                @Nonnull GitHubRepo localRepo,
                                @Nonnull GHRepository remoteRepo,
                                @Nonnull TaskListener taskListener,
                                @Nonnull GitHubSCMSource source
    ) throws IOException;
}
