package org.jenkinsci.plugins.github.pullrequest.events.impl;

import com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;

import static java.util.Objects.isNull;

/**
 * Check that PR number #isMatch() to event configured number.
 * When this happen #isSkip() defines whether PR should be skipped or trigger.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRNumber extends GitHubPREvent {
    private static final String DISPLAY_NAME = "PR Number";

    private boolean skip;
    private Integer number;
    private boolean match;

    @DataBoundConstructor
    public GitHubPRNumber(Integer number, boolean match, boolean skip) {
        this.number = number;
        this.match = match;
        this.skip = skip;
    }

    @CheckForNull
    public Integer getNumber() {
        return number;
    }

    public boolean isMatch() {
        return match;
    }

    public boolean isSkip() {
        return skip;
    }

    @Override
    public GitHubPRCause check(@Nonnull GitHubPRDecisionContext prDecisionContext) throws IOException {
        TaskListener listener = prDecisionContext.getListener();
        GHPullRequest remotePR = prDecisionContext.getRemotePR();

        if (isNull(number)) {
            // skip the whole PR because we can't trust in other checks to not get unexpected triggers.
            listener.error(DISPLAY_NAME + ": number is null -> Bad configured event, skipping other checks.");
            return prDecisionContext.newCause("Bad configured " + DISPLAY_NAME + " event.", true);
        }
        // don't know whether it can happen, but let's be safe.
        if (isNull(remotePR)) {
            // skip the whole PR because we can't trust in other checks to not get unexpected triggers.
            listener.error(DISPLAY_NAME + ": number is null -> Bad configured event, skipping other checks.");
            return prDecisionContext.newCause("Bad configured " + DISPLAY_NAME + " event.", true);
        }

        if (remotePR.getNumber() == getNumber()) {
            if (match) {
                return prDecisionContext.newCause("PR Number is matching #" + remotePR.getNumber(), isSkip());
            }
        } else if (!match) {
            return prDecisionContext.newCause("PR Number is not matching #" + remotePR.getNumber(), isSkip());
        }

        return null;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
