package com.github.kostyasha.github.integration.branch.webhook;

import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import hudson.Extension;
import hudson.model.Job;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.util.FluentIterableWrapper;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.kostyasha.github.integration.branch.webhook.WebhookInfoBranchPredicates.withBranchTrigger;
import static com.github.kostyasha.github.integration.branch.webhook.WebhookInfoBranchPredicates.withBranchTriggerRepo;
import static com.google.common.collect.Sets.immutableEnumSet;
import static java.lang.String.format;
import static net.sf.json.JSONObject.fromObject;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.triggerFrom;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isBuildable;

/**
 * @author Kanstantsin Shautsou
 */
@Extension
public class GHBranchSubscriber extends GHEventsSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(GHBranchSubscriber.class);

    @Override
    protected boolean isApplicable(Job<?, ?> job) {
        return withBranchTrigger().apply(job);
    }

    @Override
    protected Set<GHEvent> events() {
        return immutableEnumSet(GHEvent.PUSH, GHEvent.CREATE, GHEvent.DELETE);
    }

    @Override
    protected void onEvent(GHEvent event, String payload) {
        try {
            GitHub gh = GitHub.connectAnonymously();

            RefInfo ref = extractRefInfo(event, payload, gh);

            for (Job job : getJobs(ref.getRepo())) {
                GitHubBranchTrigger trigger = triggerFrom(job, GitHubBranchTrigger.class);
                if (isNull(trigger)) {
                    continue;
                }

                GitHubPRTriggerMode triggerMode = trigger.getTriggerMode();

                switch (triggerMode) {
                    case HEAVY_HOOKS_CRON:
                    case HEAVY_HOOKS: {
                        LOGGER.debug("Queued check for {} (Branch {}) after heavy hook", job.getName(),
                                ref.getRefName());
                        trigger.queueRun(job, ref.getRefName());
                        break;
                    }
                    case LIGHT_HOOKS: {
                        LOGGER.warn("Unsupported LIGHT_HOOKS trigger mode");
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

    private RefInfo extractRefInfo(GHEvent event, String payload, GitHub gh) throws IOException {
        JSONObject json = fromObject(payload);
        switch (event) {
            case CREATE: {
                return fromJson(json);
            }

            case PUSH: {
                return fromJson(json);
            }

            case DELETE: {
                return fromJson(json);
            }

            default:
                throw new IllegalStateException(format("Did you add event %s in events() method?", event));
        }
    }

    private Set<Job> getJobs(final String repo) {
        final Set<Job> ret = new HashSet<>();

        ACL.impersonate(ACL.SYSTEM, new Runnable() {
            @Override
            public void run() {
                List<Job> jobs = Jenkins.getActiveInstance().getAllItems(Job.class);
                ret.addAll(FluentIterableWrapper.from(jobs)
                        .filter(isBuildable())
                        .filter(withBranchTrigger())
                        .filter(withBranchTriggerRepo(repo))
                        .toSet()
                );
            }
        });

        return ret;
    }

    private RefInfo fromJson(JSONObject json) {
        final String repo = json.getJSONObject("repository").getString("full_name");
        final String ref_type = json.getString("ref_type");
        String ref = json.getString("ref");
        if (ref_type.equals("branch")) {
            ref = "refs/heads/" + ref;
        }
        return new RefInfo(repo, ref_type, ref);
    }
}
