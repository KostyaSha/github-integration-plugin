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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Pattern;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

/**
 * Trigger PR based on comment pattern.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRCommentEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Comment matched to pattern";
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRCommentEvent.class);

    private String comment = "";

    public String getComment() {
        return comment;
    }

    @DataBoundConstructor
    public GitHubPRCommentEvent(String comment) {
        this.comment = comment;
    }

    @Override
    public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR,
                               @CheckForNull GitHubPRPullRequest localPR, TaskListener listener) {
        if (isNull(localPR) || isNull(localPR.getLastCommentCreatedAt())) {
            return null; // nothing to compare
        }
        final PrintStream logger = listener.getLogger();

        GitHubPRCause cause = null;
        try {
            for (GHIssueComment issueComment : remotePR.getComments()) {
                if (localPR.getLastCommentCreatedAt().compareTo(issueComment.getCreatedAt()) < 0) {
                    logger.println(DISPLAY_NAME + ": state has changed (new comment found - \""
                            + issueComment.getBody() + "\")");
                    cause = checkComment(issueComment, gitHubPRTrigger.getUserRestriction(), remotePR);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Couldn't obtain comments: {}", e);
            listener.error("Couldn't obtain comments", e);
        }
        return cause;
    }

    private GitHubPRCause checkComment(GHIssueComment issueComment,
                                       GitHubPRUserRestriction userRestriction,
                                       GHPullRequest remotePR) {
        GitHubPRCause cause = null;
        try {
            String body = issueComment.getBody();

            if ((isNull(userRestriction) || userRestriction.isWhitelisted(issueComment.getUser()))
                    && Pattern.compile(comment).matcher(body).matches()) {
                LOGGER.trace("Triggering by comment '{}'", body);
                cause = new GitHubPRCause(remotePR, "PR was triggered by comment", false);
            }
        } catch (IOException ex) {
            LOGGER.error("Couldn't check comment #{}", issueComment.getId(), ex);
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
