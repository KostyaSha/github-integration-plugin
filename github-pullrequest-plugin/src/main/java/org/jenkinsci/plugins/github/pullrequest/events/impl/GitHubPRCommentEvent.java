package org.jenkinsci.plugins.github.pullrequest.events.impl;

import com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Trigger PR based on comment pattern.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRCommentEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Comment matched to pattern";
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPRCommentEvent.class);

    private String comment = "";

    public String getComment() {
        return comment;
    }

    @DataBoundConstructor
    public GitHubPRCommentEvent(String comment) {
        this.comment = comment;
    }

    @Override
    public GitHubPRCause check(@NonNull GitHubPRDecisionContext prDecisionContext) {
        final TaskListener listener = prDecisionContext.getListener();
        final PrintStream llog = listener.getLogger();
        final GitHubPRPullRequest localPR = prDecisionContext.getLocalPR();
        final GHPullRequest remotePR = prDecisionContext.getRemotePR();
        final GitHubPRUserRestriction prUserRestriction = prDecisionContext.getPrUserRestriction();

        GitHubPRCause cause = null;
        try {
            for (GHIssueComment issueComment : remotePR.getComments()) {
                if (isNull(localPR) // test all comments for trigger word even if we never saw PR before
                        || isNull(localPR.getLastCommentCreatedAt()) // PR was created but had no comments
                        // don't check comments that we saw before
                        || localPR.getLastCommentCreatedAt().compareTo(issueComment.getCreatedAt()) < 0) {
                    llog.printf("%s: state has changed (new comment found - '%s')%n",
                            DISPLAY_NAME, issueComment.getBody());

                    cause = checkComment(prDecisionContext, issueComment, prUserRestriction, listener);
                    if (nonNull(cause)) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Couldn't obtain comments: {}", e);
            listener.error("Couldn't obtain comments", e);
        }

        if (isNull(cause)) {
            LOG.debug("No matching comments found for {}", remotePR.getNumber());
            llog.println("No matching comments found for " + remotePR.getNumber());
        }

        return cause;
    }

    private GitHubPRCause checkComment(GitHubPRDecisionContext prDecisionContext,
                                       GHIssueComment issueComment,
                                       GitHubPRUserRestriction userRestriction,
                                       TaskListener listener) {
        GitHubPRCause cause = null;
        try {
            String body = issueComment.getBody();

            if (isNull(userRestriction) || userRestriction.isWhitelisted(issueComment.getUser())) {
                final Matcher matcher = Pattern.compile(comment).matcher(body);
                if (matcher.matches()) {
                    listener.getLogger().println(DISPLAY_NAME + ": matching comment " + body);
                    LOG.trace("Event matches comment '{}'", body);
                    cause = prDecisionContext.newCause("Comment matches to criteria.", false);
                    cause.withCommentBody(body);
                    if (matcher.groupCount() > 0) {
                        cause.withCommentBodyMatch(matcher.group(1));
                    }
                }
            }
        } catch (IOException ex) {
            LOG.error("Couldn't check comment #{}, skipping it.", issueComment.getId(), ex);
        }
        return cause;
    }

    @Symbol("commentPattern")
    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
