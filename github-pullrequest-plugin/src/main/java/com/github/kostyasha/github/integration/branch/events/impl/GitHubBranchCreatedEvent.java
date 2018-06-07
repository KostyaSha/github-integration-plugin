package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEventDescriptor;
import com.github.kostyasha.github.integration.generic.GitHubBranchDecisionContext;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.Symbol;
import org.kohsuke.github.GHBranch;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;

import static java.util.Objects.isNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Triggers when branch didn't exist before and appeared in remote.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchCreatedEvent extends GitHubBranchEvent {
    private static final String DISPLAY_NAME = "Branch Created";
    private static final Logger LOG = getLogger(GitHubBranchCreatedEvent.class);

    @DataBoundConstructor
    public GitHubBranchCreatedEvent() {
    }

    @Override
    public GitHubBranchCause check(@Nonnull GitHubBranchDecisionContext context) throws IOException {
        GHBranch remoteBranch = context.getRemoteBranch();
        GitHubBranch localBranch = context.getLocalBranch();
        TaskListener listener = context.getListener();

        if (isNull(remoteBranch) && isNull(localBranch)) {
            // just in case
            LOG.debug("Remote and local branch are null");
            return null;
        } else if (isNull(remoteBranch)) {
            LOG.debug("Remote branch is null for localBranch '{}'. Branch can't be 'created'", localBranch.getName());
            return null;
        }

        GitHubBranchCause cause = null;
        if (isNull(localBranch)) { // didn't exist before
            final PrintStream logger = listener.getLogger();
            logger.println(DISPLAY_NAME + ": '" + remoteBranch.getName() + "'");
            LOG.debug("{}: '{}'", DISPLAY_NAME, remoteBranch.getName());
            cause = context.newCause(DISPLAY_NAME, false);
        }

        return cause;
    }

    @Symbol("branchCreated")
    @Extension
    public static class DescriptorImpl extends GitHubBranchEventDescriptor {
        @Nonnull
        @Override
        public final String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
