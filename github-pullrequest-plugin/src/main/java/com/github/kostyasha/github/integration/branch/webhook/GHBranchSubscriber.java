package com.github.kostyasha.github.integration.branch.webhook;

import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.security.ACLContext;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.util.FluentIterableWrapper;
import org.kohsuke.github.GHEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob;
import static com.github.kostyasha.github.integration.branch.webhook.WebhookInfoBranchPredicates.withBranchTrigger;
import static com.github.kostyasha.github.integration.branch.webhook.WebhookInfoBranchPredicates.withBranchTriggerRepo;
import static com.google.common.collect.Sets.immutableEnumSet;
import static hudson.security.ACL.SYSTEM;
import static java.lang.String.format;
import static net.sf.json.JSONObject.fromObject;
import static java.util.Objects.isNull;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isBuildable;

/**
 * @author Kanstantsin Shautsou
 * @see org.jenkinsci.plugins.github.pullrequest.webhook.GHPullRequestSubscriber
 */
@SuppressWarnings("unused")
@Extension
public class GHBranchSubscriber extends AbstractGHBranchSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(GHBranchSubscriber.class);

    @Override
    protected boolean isApplicable(@Nullable Item item) {
        return item instanceof Job &&
                withBranchTrigger().apply((Job) item);
    }

    @Override
    protected void onEvent(GHSubscriberEvent event) {
        try {
            BranchInfo ref = extractRefInfo(event.getGHEvent(), event.getPayload());

            for (Job job : getBranchTriggerJobs(ref.getRepo())) {
                GitHubBranchTrigger trigger = ghBranchTriggerFromJob(job);
                if (isNull(trigger)) {
                    continue;
                }

                GitHubPRTriggerMode triggerMode = trigger.getTriggerMode();

                switch (triggerMode) {
                    case HEAVY_HOOKS_CRON:
                    case HEAVY_HOOKS: {
                        LOGGER.debug("Queued check for {} (Branch {}) after heavy hook", job.getName(),
                                ref.getBranchName());
                        trigger.queueRun(job, ref.getBranchName());
                        break;
                    }
                    case LIGHT_HOOKS: {
                        LOGGER.warn("Unsupported LIGHT_HOOKS trigger mode");
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("Can't process {} hook", event, e);
        }
    }

    static Set<Job> getBranchTriggerJobs(final String repo) {
        final Set<Job> ret = new HashSet<>();

        try (ACLContext ignored = ACL.as(SYSTEM)) {
            List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);
            ret.addAll(FluentIterableWrapper.from(jobs)
                    .filter(isBuildable())
                    .filter(withBranchTrigger())
                    .filter(withBranchTriggerRepo(repo))
                    .toSet()
            );
        }

        return ret;
    }

}
