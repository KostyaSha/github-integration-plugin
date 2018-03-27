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
import static com.github.kostyasha.github.integration.generic.utils.RetryableGitHubOperation.execute;
import org.kohsuke.github.GitHub;
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
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private GHPullRequestReview prr = new GHPullRequestReview();

    @DataBoundConstructor
    public GitHubPRApproved() {
    }

    @Override
    public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR,
                               GitHubPRPullRequest localPR, TaskListener listener) throws IOException {

        GitHubPRCause cause = null;

        // analyse the json file
        String path = System.getProperty("user.home") + "/.jenkins/workspace";
        File fileName = new File(path + "/pr_" + remotePR.getRepository().getName() + "_#" + String.valueOf(remotePR.getNumber()) + ".json");
        if(!fileName.exists()){
            LOG.warn("File name " + fileName.getName() + " does not exist! Impossible evaluating if the PR is approved.");
            return cause;
        }

        // check json file
        PRApprovalState pras = MAPPER.readValue(fileName,PRApprovalState.class);
        if(pras.getAction().equals("review_request_removed")){ // don't trigger the job when a reviewer is removed
            return null;
        }

        // check if all reviewers approved the PR
        boolean approved = true;
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

            // Set PR owner's email
            cause.withPROwnerEmail(remotePR.getUser().getEmail());

            // Set reviewers emails
            final GitHub root = GitHub.connect(); //Connect to GitHub API
            for(ReviewState reviewState : pras.getReviews_states()){
                try {
                    String email = execute(() -> (root.getUser(reviewState.getReviewer())).getEmail());
                    if(!isNull(email)){
                        cause.withPRReviewerEmail(email);
                    }
                } catch (Exception e) {
                    LOG.error("Can't get GitHub reviewer email: ", e);
                }
            }
        }
        
        if (isNull(cause))
            LOG.info("PR NOT approved");
        else
            LOG.info("PR approved");

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
