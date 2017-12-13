package org.jenkinsci.plugins.github.pullrequest.webhook;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.util.FluentIterableWrapper;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHEventPayload.IssueComment;
import org.kohsuke.github.GHEventPayload.PullRequest;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    protected boolean isApplicable(Item item) {
        return item instanceof Job && withPRTrigger().apply((Job) item);
    }

    @Override
    protected Set<GHEvent> events() {
        return immutableEnumSet(GHEvent.PULL_REQUEST, GHEvent.ISSUE_COMMENT);
    }

    @Override
    protected void onEvent(GHSubscriberEvent event) {
        try {
            GitHub gh = GitHub.connectAnonymously();

            PullRequestInfo info = extractPullRequestInfo(event.getGHEvent(), event.getPayload(), gh);

            for (Job job : getPRTriggerJobs(info.getRepo())) {
                GitHubPRTrigger trigger = ghPRTriggerFromJob(job);
                GitHubPRTriggerMode triggerMode = trigger.getTriggerMode();

                switch (triggerMode) {
                    case HEAVY_HOOKS_CRON:
                    case HEAVY_HOOKS: {
                        LOGGER.debug("Queued check for {} (PR #{}) after heavy hook", job.getName(), info.getNum());
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
        switch (event) {
            case ISSUE_COMMENT: {
                IssueComment commentPayload = gh.parseEventPayload(new StringReader(payload), IssueComment.class);

                int prNumber = commentPayload.getIssue().getNumber();

                return new PullRequestInfo(commentPayload.getRepository().getFullName(), prNumber);
            }

            case PULL_REQUEST: {
                PullRequest pr = gh.parseEventPayload(new StringReader(payload), PullRequest.class);

                return new PullRequestInfo(pr.getPullRequest().getRepository().getFullName(), pr.getNumber());
            }

            default:
                throw new IllegalStateException(format("Did you add event %s in events() method?", event));
        }
    }

    static Set<Job> getPRTriggerJobs(final String repo) {
        final Set<Job> ret = new HashSet<>();

        ACL.impersonate(ACL.SYSTEM, () -> {
            List<Job> jobs = Jenkins.getActiveInstance().getAllItems(Job.class);
            ret.addAll(FluentIterableWrapper.from(jobs)
                    .filter(isBuildable())
                    .filter(withPRTrigger())
                    .filter(withRepo(repo))
                    .toSet()
            );
        });

        return ret;
    }
}
