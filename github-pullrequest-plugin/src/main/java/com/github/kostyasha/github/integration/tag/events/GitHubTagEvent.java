package com.github.kostyasha.github.integration.tag.events;

import com.github.kostyasha.github.integration.generic.GitHubTagDecisionContext;
import com.github.kostyasha.github.integration.tag.GitHubTagCause;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Different "events" that may want trigger run for tag.
 *
 * @author Kanstantsin Shautsou
 * @see com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent
 */
public abstract class GitHubTagEvent extends AbstractDescribableImpl<GitHubTagEvent> implements ExtensionPoint {

    /**
     * indicates that branch was created
     *
     * @return cause object. null when no influence (other events will be checked.
     * If cause.isSkip() == true, then other checks wouldn't influence. And triggering for this branch will be skipped.
     * If cause.isSkip() == false, indicates that branch build should be run.
     */
    @CheckForNull
    public GitHubTagCause check(@Nonnull GitHubTagDecisionContext context) throws IOException {
        return null;
    }

    @Override
    public GitHubTagEventDescriptor getDescriptor() {
        return (GitHubTagEventDescriptor) super.getDescriptor();
    }
}
