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

        GitHubPRCause cause = null;
        LOG.warn("Checking\n\n\n");

        // analyse the json file
        String home = System.getProperty("user.home");
        File fileName = new File(home + "/pr_" + remotePR.getRepository().getName() + "_#" + String.valueOf(remotePR.getNumber()) + ".json");
        if(!fileName.exists()){
            return cause;
        }

        // check json file
        boolean approved = true;
        ObjectMapper objectMapper = new ObjectMapper();
        PRApprovalState pras = objectMapper.readValue(fileName,PRApprovalState.class);
        for( ReviewState r : pras.getReviews_states() ){
            if(!r.getState().equals("approved")){
                approved = false;
                break;
            }
        }

        // If there are no reviewers we consider the PR as not accepted
        if(pras.getReviews_states().size() == 0)
            approved = false;

        // If approved
        if (approved){
            final PrintStream logger = listener.getLogger();
            logger.println(DISPLAY_NAME + ": state has changed (PR was approved)");
            cause = new GitHubPRCause(remotePR, "PR was approved, triggered because: " + pras.getAction() + " commit hash: " + remotePR.getHead().getSha(), false); 
        }
        
        if (isNull(cause))
            LOG.info("PR NOT approved\n\n\n");
        else
            LOG.info("PR approved\n\n\n");

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
