package org.jenkinsci.plugins.github.pullrequest.webhook;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import hudson.Extension;
import hudson.model.Job;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import com.github.kostyasha.github.integration.generic.GitHubTriggerDescriptor;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.util.FluentIterableWrapper;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GHEventPayload.IssueComment;
import org.kohsuke.github.GHEventPayload.PullRequest;
import org.kohsuke.github.GHEventPayload.PullRequestReview;
import org.kohsuke.github.GitHub;
import org.jenkinsci.plugins.github.pullrequest.utils.PRApprovalState;
import org.jenkinsci.plugins.github.pullrequest.utils.ReviewState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.io.File;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Sets.immutableEnumSet;
import static java.lang.String.format;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromJob;
import static org.jenkinsci.plugins.github.pullrequest.webhook.WebhookInfoPredicates.withPRTrigger;
import static org.jenkinsci.plugins.github.pullrequest.webhook.WebhookInfoPredicates.withRepo;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isBuildable;

/**
 * Uses extension point from github-plugin to get events form standard github-webhook endpoint.
 * Subscribes on pull_request and issue_comment events.
 *
 * @author lanwen (Merkushev Kirill)
 */
@SuppressWarnings("unused")
@Extension
public class GHPullRequestSubscriber extends GHEventsSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(GHPullRequestSubscriber.class);
    private static final String PATH = System.getProperty("user.home") + "/.jenkins3/workspace";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected boolean isApplicable(Job<?, ?> job) {
        return withPRTrigger().apply(job);
    }

    @Override
    protected Set<GHEvent> events() {
        return immutableEnumSet(GHEvent.PULL_REQUEST, GHEvent.ISSUE_COMMENT, GHEvent.PULL_REQUEST_REVIEW);
    }

    @Override
    protected void onEvent(GHEvent event, String payload) {
        try {
            GitHub gh = GitHub.connectAnonymously();

            PullRequestInfo info = extractPullRequestInfo(event, payload, gh);

            // For each hob that has the current repo, gets the trigger
            // 
            for (Job job : getPRTriggerJobs(info.getRepo())) {
                LOGGER.warn("Job name: " + job.getName());
                GitHubPRTrigger trigger = ghPRTriggerFromJob(job);
                GitHubPRTriggerMode triggerMode = trigger.getTriggerMode();               
                
                switch (triggerMode) {
                    case HEAVY_HOOKS_CRON:
                    case HEAVY_HOOKS: {
                        LOGGER.debug("Queued check for {} (PR #{}) after heavy hook", job.getName(), info.getNum());
                        //Trigger the job
                        trigger.queueRun(job, info.getNum());
                        break;
                    }
                    case LIGHT_HOOKS: {
                        LOGGER.warn("Unsupported LIGHT_HOOKS trigger mode");
//                        LOGGER.info("Begin processing hooks for {}", trigger.getRepoFullName(job));
//                        for (GitHubPREvent prEvent : trigger.getEvents()) {
//                            GitHubPRCause cause = prEvent.checkHook(trigger, parsedPayload, null);
//                            if (cause != null) {
//                                trigger.build(cause);
//                            }
//                        }
                        break;
                    }
                    default:
                        break;
                }
            }

        } catch (Exception e) {
            LOGGER.error("Can't process {} hook", event, e);
        }
    }

    private PullRequestInfo extractPullRequestInfo(GHEvent event, String payload, GitHub gh) throws IOException {
        LOGGER.debug("Payload:\n" + payload);

        switch (event) {
            case ISSUE_COMMENT: {
                IssueComment commentPayload = gh.parseEventPayload(new StringReader(payload), IssueComment.class);
                int prNumber = commentPayload.getIssue().getNumber();

                return new PullRequestInfo(commentPayload.getRepository().getFullName(), prNumber);
            }

            case PULL_REQUEST: {
                PullRequest pr = gh.parseEventPayload(new StringReader(payload), PullRequest.class);

                File fileName = new File(PATH + "/pr_" + pr.getRepository().getName() + "_#" + String.valueOf(pr.getNumber()) + ".json");

                // If the PR's action is not review_requested or *_removed it does not contain the field
                // requested_reviewers and throws an error.
                if(!pr.getAction().equals("review_requested") && !pr.getAction().equals("review_request_removed")){
                    if(fileName.exists()){
                        // If the PR is closed we delete the file
                        if(pr.getAction().equals("closed")){
                            if(fileName.delete()){
                                LOGGER.info("PR is closed, file " + fileName.getName() + " deleted successfully");
                            }
                            else{
                                LOGGER.warn("PR is closed, impossible deleting file " + fileName.getName());
                            }
                        }
                        else{ 
                            // Update the action in the json file
                            PRApprovalState pras = MAPPER.readValue(fileName,PRApprovalState.class);
                            pras.setAction(pr.getAction());
                            MAPPER.writeValue(fileName,pras);
                        }
                    }
                    return new PullRequestInfo(pr.getPullRequest().getRepository().getFullName(), pr.getNumber());
                }

                PRApprovalState pras = new PRApprovalState();
                // Get the reviewers and add the reviewer specified in the payload.
                if(fileName.exists()){
                    pras = MAPPER.readValue(fileName,PRApprovalState.class);
                }

                List<ReviewState> rs = new ArrayList<ReviewState>();
                if(pr.getAction().equals("review_requested")){
                
                    // Add existing reviewers
                    for(ReviewState reviewState : pras.getReviews_states()){
                        rs.add(reviewState);
                    }
         
                    for(GHUser user : pr.getPullRequest().getRequestedReviewers()){
                        // check for duplicates
                        // Notice: sometimes github might not send you all the reviewers at once but just the requested one.
                        boolean duplicate = false;
                        for(ReviewState review : rs){
                            if(user.getLogin().equals(review.getReviewer()) ){
                                duplicate = true;
                                break;
                            }
                        }
                        if(!duplicate){
                            LOGGER.info("Added reviewer: {} ", user.getLogin());
                            rs.add(new ReviewState(user.getLogin(),user.getEmail()));
                        }
                    }
                }
       
                // Remove the reviewer from the json file.
                if(pr.getAction().equals("review_request_removed")){
                    rs = pras.getReviews_states();
                    // Remove reviewers 
                    for (Iterator<ReviewState> iter = rs.listIterator(); iter.hasNext(); ){
                        ReviewState reviewState = iter.next();
                        if(pr.getRequestedReviewer().getLogin().equals(reviewState.getReviewer())){
                            iter.remove();
                        }
                    }
                }

                pras.setAction(pr.getAction());
                pras.setReviews_states(rs);
                
                try{
                    MAPPER.writeValue(fileName,pras);
                }
                catch(java.lang.NullPointerException e){
                    LOGGER.error("Exception writing the PRApprovalObject: " + e.getMessage());
                }
              
                return new PullRequestInfo(pr.getPullRequest().getRepository().getFullName(), pr.getNumber());
            }

            case PULL_REQUEST_REVIEW: {
                PullRequestReview prr = gh.parseEventPayload(new StringReader(payload), PullRequestReview.class);

                //Read the file 
                File fileName = new File(PATH + "/pr_" + prr.getRepository().getName() + "_#" + String.valueOf(prr.getPullRequest().getNumber()) + ".json");

                // Update PR state 
                PRApprovalState pras;
                pras = MAPPER.readValue(fileName,PRApprovalState.class);
                List<ReviewState> reviewStates = pras.getReviews_states();

                // Update the state of the current reviewer
                pras.setAction(prr.getAction());
                for( int i = 0; i < reviewStates.size(); i++ ){
                    if(reviewStates.get(i).getReviewer().equals(prr.getReview().getUser().getLogin()) ){
                        reviewStates.get(i).setState(prr.getReview().getState());
                    }
                }
                pras.setReviews_states(reviewStates);
                try{
                    MAPPER.writeValue(fileName,pras);
                }
                catch(java.lang.NullPointerException e){
                    LOGGER.error("Exception writing the PRApprovalObject: " + e.getMessage());
                }
                // Here we have to return the name of the repo.
                return new PullRequestInfo(prr.getPullRequest().getRepository().getFullName(), prr.getPullRequest().getNumber());
            }
            default:
                throw new IllegalStateException(format("Did you add event %s in events() method?", event));
        }
    }

    /*
    * This method filters the jobs depending on the trigger
    */
    static Set<Job> getPRTriggerJobs(final String repo) {
        final Set<Job> ret = new HashSet<>();

        ACL.impersonate(ACL.SYSTEM, () -> {
            List<Job> jobs = Jenkins.getActiveInstance().getAllItems(Job.class);
            ret.addAll(FluentIterableWrapper.from(jobs)
                    .filter(isBuildable())
                    .filter(withPRTrigger()) // Filters by trigger
                    .filter(withRepo(repo))
                    .toSet()
            );
        });

        return ret;
    }
}
