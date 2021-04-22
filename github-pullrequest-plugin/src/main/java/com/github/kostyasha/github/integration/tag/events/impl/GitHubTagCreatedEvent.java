package com.github.kostyasha.github.integration.tag.events.impl;

import com.github.kostyasha.github.integration.generic.GitHubTagDecisionContext;
import com.github.kostyasha.github.integration.tag.GitHubTag;
import com.github.kostyasha.github.integration.tag.GitHubTagCause;
import com.github.kostyasha.github.integration.tag.GitHubTagRepository;
import com.github.kostyasha.github.integration.tag.events.GitHubTagEvent;
import com.github.kostyasha.github.integration.tag.events.GitHubTagEventDescriptor;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.Symbol;
import org.kohsuke.github.GHTag;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.PrintStream;

import static java.util.Objects.isNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Triggers when tag didn't exist before and appeared in remote.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubTagCreatedEvent extends GitHubTagEvent {
    private static final String DISPLAY_NAME = "Tag Created";
    private static final Logger LOG = getLogger(GitHubTagCreatedEvent.class);

    @DataBoundConstructor
    public GitHubTagCreatedEvent() {
    }

    @Override
    public GitHubTagCause check(@NonNull GitHubTagDecisionContext context) throws IOException {
        GHTag remoteTag = context.getRemoteTag();
        GitHubTag localTag = context.getLocalTag();
        GitHubTagRepository localRepo = context.getLocalRepo();
        TaskListener listener = context.getListener();

        if (remoteTag == null && localTag == null) {
            // just in case
            LOG.debug("Remote and local tag are null");
            return null;
        } else if (remoteTag == null) {
            LOG.debug("Remote tag is null for localTag '{}'. Tag can't be 'created'", localTag.getName());
            return null;
        }

        GitHubTagCause cause = null;
        if (isNull(localTag)) { // didn't exist before
            final PrintStream logger = listener.getLogger();
            logger.println(DISPLAY_NAME + ": '" + remoteTag.getName() + "'");
            LOG.debug("{}: '{}'", DISPLAY_NAME, remoteTag.getName());
            cause = context.newCause(DISPLAY_NAME, false);
        }

        return cause;
    }

    @Symbol("created")
    @Extension
    public static class DescriptorImpl extends GitHubTagEventDescriptor {
        @NonNull
        @Override
        public final String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
