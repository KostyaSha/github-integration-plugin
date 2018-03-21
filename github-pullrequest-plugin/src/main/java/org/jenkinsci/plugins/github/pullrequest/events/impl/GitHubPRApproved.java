package org.jenkinsci.plugins.github.pullrequest.events.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.jenkinsci.plugins.github.pullrequest.utils.PRApprovalState;
import org.jenkinsci.plugins.github.pullrequest.utils.ReviewState;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReview;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.PrintStream;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
    
/**
 * When PR is approved
 *
 * @author Nicola Covallero
 */
public class GitHubPRApproved extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Pull Request Approved";
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPRApproved.class); //NOPMD
    private GHPullRequestReview prr = new GHPullRequestReview();

    @DataBoundConstructor
    public GitHubPRApproved() {
    }

    @Override
    public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR,
                               GitHubPRPullRequest localPR, TaskListener listener) throws IOException {
        // if (isNull(localPR)) {
        //     return null;
        // }

        //LOG.warn("Running check\n\n\n");
        //GitHubPRCause cause = null;

        // // analyse the json file
        // String home = System.getProperty("user.home");
        // File fileName = new File(home + "/pr_" + remotePR.getRepository().getName() + "_#" + String.valueOf(remotePR.getNumber()) + ".json");

        // LOG.warn("Analyse json\n\n\n");
        // // check json file
        // boolean approved = true;
        // ObjectMapper objectMapper = new ObjectMapper();
        // PRApprovalState pras = objectMapper.readValue(fileName,PRApprovalState.class);
        // for( ReviewState r : pras.getReviews_states() ){
        //     if(!r.getState().equals("approved")){
        //         approved = false;
        //         break;
        //     }
        // }
        // // If there are no reviewers we consider the PR as not accepted
        // if(pras.getReviews_states().size() == 0)
        //     approved = false;
        // LOG.warn("Finished analysing json\n\n\n");

        // // If approved
        // if (approved){
        //     final PrintStream logger = listener.getLogger();
        //     logger.println(DISPLAY_NAME + ": state has changed (PR was approved)");
        //     cause = new GitHubPRCause(remotePR, "PR was approved", false);
        //     LOG.warn("Approved\n\n\n");    
        // }
        
        // if (isNull(cause))
        //     LOG.warn("PR NOT approved\n\n\n");
        // else
        //     LOG.warn("PR approved\n\n\n");
        if (isNull(localPR)) {
            return null;
        }

        GitHubPRCause cause = null;

        // must be closed once
        if (remotePR.getState().equals(GHIssueState.CLOSED)) {
            final PrintStream logger = listener.getLogger();
            logger.println(DISPLAY_NAME + ": state has changed (PR was closed)");
            cause = new GitHubPRCause(remotePR, "PR was closed", false);
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
