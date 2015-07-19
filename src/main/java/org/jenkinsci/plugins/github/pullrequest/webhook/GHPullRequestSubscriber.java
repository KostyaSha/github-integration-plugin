package org.jenkinsci.plugins.github.pullrequest.webhook;

import com.google.common.base.Predicate;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
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
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isBuildable;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.withTrigger;

/**
 * Uses extension point from github-plugin to get events form standard github-webhook endpoint.
 * Subscribes on pull_request and issue_comment events.
 * 
 * @author lanwen (Merkushev Kirill)
 */
@SuppressWarnings("unused")
@Extension
public class GHPullRequestSubscriber extends GHEventsSubscriber {
    private static Logger LOGGER = LoggerFactory.getLogger(GHPullRequestSubscriber.class);

    @Override
    protected boolean isApplicable(AbstractProject<?, ?> abstractProject) {
        return withTrigger(GitHubPRTrigger.class).apply(abstractProject);
    }

    @Override
    protected Set<GHEvent> events() {
        return immutableEnumSet(GHEvent.PULL_REQUEST, GHEvent.ISSUE_COMMENT);
    }

    @Override
    protected void onEvent(GHEvent event, String payload) {
        try {
            GitHub gh = ((GitHubPRTrigger.DescriptorImpl) Jenkins.getInstance()
                    .getDescriptorOrDie(GitHubPRTrigger.class)).getGitHub();

            PullRequestInfo info = extractPullRequestInfo(event, payload, gh);

            for (AbstractProject<?, ?> job : getJobs(info.getRepo())) {
                GitHubPRTrigger trigger = job.getTrigger(GitHubPRTrigger.class);
                GitHubPRTriggerMode triggerMode = trigger.getTriggerMode();

                switch (triggerMode) {
                    case HEAVY_HOOKS:
                        trigger.queueRun(job, info.getNum());
                        break;

                    case LIGHT_HOOKS:
                        LOGGER.warn("Unsupported LIGHT_HOOKS trigger mode");
//                        LOGGER.log(Level.INFO, "Begin processing hooks for {0}", trigger.getRepoFullName(job));
//                        for (GitHubPREvent prEvent : trigger.getEvents()) {
//                            GitHubPRCause cause = prEvent.checkHook(trigger, parsedPayload, null);
//                            if (cause != null) {
//                                trigger.build(cause);
//                            }
//                        }
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

    private Set<AbstractProject> getJobs(final String repo) {
        final Set<AbstractProject> ret = new HashSet<>();

        ACL.impersonate(ACL.SYSTEM, new Runnable() {
            @Override
            public void run() {
                List<AbstractProject> jobs = Jenkins.getInstance().getAllItems(AbstractProject.class);
                ret.addAll(FluentIterableWrapper.from(jobs)
                        .filter(isBuildable())
                        .filter(withTrigger(GitHubPRTrigger.class))
                        .filter(withRepo(repo)).toSet());
            }
        });

        return ret;
    }

    private Predicate<AbstractProject> withRepo(final String repo) {
        return new Predicate<AbstractProject>() {
            @Override
            public boolean apply(AbstractProject job) {
                GitHubPRTrigger trigger = (GitHubPRTrigger) job.getTrigger(GitHubPRTrigger.class);
                return trigger.getTriggerMode() != null
                        && equalsIgnoreCase(repo, trigger.getRepoFullName(job));
            }
        };
    }
}
