package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Trigger/skip PR based on comment pattern
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRCommentEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Comment matched to pattern";
    private final static Logger LOGGER = Logger.getLogger(GitHubPRCommentEvent.class.getName());

    private String comment = "";

    private boolean skip;

    public String getComment() {
        return comment;
    }

    public boolean isSkip() {
        return skip;
    }

    @DataBoundConstructor
    public GitHubPRCommentEvent(String comment, boolean skip) {
        this.comment = comment;
        this.skip = skip;
    }

    @Override
    public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR,
                               @CheckForNull GitHubPRPullRequest localPR, TaskListener listener) {
        if (localPR == null || localPR.getLastCommentCreatedAt() == null) {
            return null; // nothing to compare
        }
        final PrintStream logger = listener.getLogger();

//        LOGGER.log(Level.FINE, "Checking for new messages...");

        GitHubPRCause cause = null;
        try {
            for (GHIssueComment comment : remotePR.getComments()) {
                if (localPR.getLastCommentCreatedAt().compareTo(comment.getCreatedAt()) < 0) {
                    logger.println(DISPLAY_NAME + ": state has changed (new comment found - \""
                            + comment.getBody() + "\")");
                    cause = checkComment(comment, gitHubPRTrigger.getUserRestriction(), remotePR);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Couldn''t obtain comments: {0}", e.getMessage());
        }
        return cause;
    }

    private GitHubPRCause checkComment(GHIssueComment comment,
                                       GitHubPRUserRestriction userRestriction,
                                       GHPullRequest remotePR) {
        GitHubPRCause cause = null;
        try {
            String body = comment.getBody();

            if ((userRestriction == null || userRestriction.isWhitelisted(comment.getUser()))
                    && Pattern.compile(this.comment).matcher(body).matches()) {
                LOGGER.log(Level.FINEST, "Triggering by comment '{0}'", body);
                cause = new GitHubPRCause(remotePR, "PR was triggered by comment", isSkip());
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Couldn't check comment #" + comment.getId(), ex);
        }
        return cause;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
