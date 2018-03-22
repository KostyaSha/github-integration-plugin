package org.jenkinsci.plugins.github.pullrequest.webhook;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import hudson.Extension;
import hudson.model.Job;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
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

    @Override
    protected boolean isApplicable(Job<?, ?> job) {
        return withPRTrigger().apply(job);
    }

    @Override
    protected Set<GHEvent> events() {
        // TODO add push event
        return immutableEnumSet(GHEvent.PULL_REQUEST, GHEvent.ISSUE_COMMENT, GHEvent.PULL_REQUEST_REVIEW,GHEvent.PULL_REQUEST_REVIEW_COMMENT);
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
        LOGGER.warn("extractPullRequestInfo(), even: " + event);
        switch (event) {
            case ISSUE_COMMENT: {
                IssueComment commentPayload = gh.parseEventPayload(new StringReader(payload), IssueComment.class);
                int prNumber = commentPayload.getIssue().getNumber();

                return new PullRequestInfo(commentPayload.getRepository().getFullName(), prNumber);
            }

            case PULL_REQUEST: {
                LOGGER.warn("\nParsing the pull request\n");
                PullRequest pr = gh.parseEventPayload(new StringReader(payload), PullRequest.class);

                List<GHUser> u = pr.getPullRequest().getRequestedReviewers();

                if(u.size() == 0){
                    LOGGER.warn("\nNo requested reviewers\n");
                }
                
                PRApprovalState pras = new PRApprovalState();
                List<ReviewState> rs = new ArrayList<ReviewState>();
                for(int i = 0; i < u.size() ; i++){
                    LOGGER.warn("reviewer {}: {} ", i, u.get(i).getLogin());
                    rs.add(new ReviewState(u.get(i).getLogin()));
                }
                pras.setReviews_states(rs);
                
                ObjectMapper objectMapper = new ObjectMapper();
                // LOGGER.warn(objectMapper.writeValueAsString(pras));
                try{
                    String home = System.getProperty("user.home");
                    File fileName = new File(home + "/pr_" + pr.getRepository().getName() + "_#" + String.valueOf(pr.getNumber()) + ".json");
                    objectMapper.writeValue(fileName,pras);
                    TimeUnit.SECONDS.sleep(2);//wait 2 seconds to give time to fully write the .json
                }
                catch(java.lang.NullPointerException e){
                    LOGGER.warn("Exception writin the PRApprovalObject: " + e.getMessage());
                }
                catch(java.lang.InterruptedException e){
                    LOGGER.warn("Exception writin the PRApprovalObject: " + e.getMessage());
                }
              
                return new PullRequestInfo(pr.getPullRequest().getRepository().getFullName(), pr.getNumber());
            }

            case PULL_REQUEST_REVIEW: {
                PullRequestReview prr = gh.parseEventPayload(new StringReader(payload), PullRequestReview.class);

                //Read the file 
                String home = System.getProperty("user.home");
                File fileName = new File(home + "/pr_" + prr.getRepository().getName() + "_#" + String.valueOf(prr.getPullRequest().getNumber()) + ".json");

                // Update PR state 
                // So far it just update the state of the current reviewer. 
                ObjectMapper objectMapper = new ObjectMapper();
                PRApprovalState pras = objectMapper.readValue(fileName,PRApprovalState.class);
                List<ReviewState> reviewStates = pras.getReviews_states();

                // Update the state of the current reviewer
                for( int i = 0; i < reviewStates.size(); i++ ){
                    if(reviewStates.get(i).getReviewer().equals(prr.getReview().getUser().getLogin()) ){
                        reviewStates.get(i).setState(prr.getReview().getState());
                    }
                }
                pras.setReviews_states(reviewStates);
                LOGGER.warn(objectMapper.writeValueAsString(pras));
                objectMapper.writeValue(fileName,pras);

                // Here we have to return the name of the repo.
                return new PullRequestInfo(prr.getPullRequest().getRepository().getFullName(), prr.getPullRequest().getNumber());
            }
            case PULL_REQUEST_REVIEW_COMMENT: {
                PullRequestReview prr = gh.parseEventPayload(new StringReader(payload), PullRequestReview.class);
                LOGGER.warn("\n\n PullRequestReviewComment received \n\n" + payload);
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
