package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPRReviewEvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
//import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReview;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
//import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

/**
 * When PR is approved
 *
 * @author Nicola Covallero
 */
public class GitHubPRApproved extends GitHubPRReviewEvent {
    private static final String DISPLAY_NAME = "Pull Request Approved";
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPRApproved.class); //NOPMD

    private GitHubPRPullRequest pr;
    private PagedIterable<GHPullRequestReview> reviews;

    @DataBoundConstructor
    public GitHubPRApproved() {
    }

    // @Override
    // public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR,
    //                            GitHubPRPullRequest localPR, TaskListener listener) throws IOException {
    //     if (isNull(localPR)) {
    //         return null;
    //     }

    //     GitHubPRCause cause = null;

    //     // must be closed once
    //     // if (remotePR.getState().equals(GHIssueState.CLOSED)) {
    //     //     final PrintStream logger = listener.getLogger();
    //     //     logger.println(DISPLAY_NAME + ": state has changed (PR was closed)");
    //     //     cause = new GitHubPRCause(remotePR, "PR was accept", false);
    //     // }

    //     return cause;
    // }

    // TODO:
    // 1 - get reviewers
    //
    //

    @Override
    public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger, GHPullRequestReview remotePRR,
                            GitHubPRPullRequest localPR, TaskListener listener) throws IOException {
        if (isNull(localPR)) {
            return null;
        }

        GitHubPRCause cause = null;

        pr = new GitHubPRPullRequest(remotePRR.getParent());
        reviews = remotePRR.getParent().listReviews();

        // must be closed once
        // if (remotePR.getState().equals(GHIssueState.CLOSED)) {
        //     final PrintStream logger = listener.getLogger();
        //     logger.println(DISPLAY_NAME + ": state has changed (PR was closed)");
        //     cause = new GitHubPRCause(remotePR, "PR was accept", false);
        // }

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
