package com.github.kostyasha.github.integration.multibranch.hooks;

import com.github.kostyasha.github.integration.branch.webhook.AbstractGHBranchSubscriber;
import com.github.kostyasha.github.integration.branch.webhook.BranchInfo;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubBranchHandler;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubHandler;
import hudson.Extension;
import hudson.model.Item;
import jenkins.scm.api.SCMEvent;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * @author Kanstantsin Shautsou
 * @see GitHubBranchHandler
 */
@SuppressWarnings("unused")
@Extension
public class GHMultiBranchSubscriber extends AbstractGHBranchSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(GHMultiBranchSubscriber.class);

    @Override
    protected boolean isApplicable(@Nullable Item item) {
        if (item instanceof SCMSourceOwner) {
            SCMSourceOwner scmSourceOwner = (SCMSourceOwner) item;
            for (SCMSource source : scmSourceOwner.getSCMSources()) {
                if (source instanceof GitHubSCMSource) {
                    GitHubSCMSource gitHubSCMSource = (GitHubSCMSource) source;
                    for (GitHubHandler hubHandler : gitHubSCMSource.getHandlers()) {
                        if (hubHandler instanceof GitHubBranchHandler) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    protected void onEvent(GHSubscriberEvent event) {
        try {
            BranchInfo ref = extractRefInfo(event.getGHEvent(), event.getPayload());
            SCMHeadEvent.fireNow(new GitHubSCMHeadEvent(
                    SCMEvent.Type.UPDATED,
                    event.getTimestamp(),
                    ref,
                    ref.getRepo())
            );

//            final Set<MultiBranchProject> ret = new HashSet<>();
//
//            try (ACLContext ignored = ACL.as(SYSTEM)) {
//                List<MultiBranchProject> jobs = Jenkins.getInstance().getAllItems(MultiBranchProject.class);
//                ret.addAll(jobs.stream()
//                        .filter(isBuildable())
////                        .filter(withGitHubSCMSource(ref.getRepo()))
//                        .filter(withBranchHandler())
//                        .collect(Collectors.toSet())
//                );
//            }

//            for (MultiBranchProject job : ret) {
//                GitHubBranchTrigger trigger = ghBranchTriggerFromJob(job);
//                if (isNull(trigger)) {
//                    continue;
//                }
//
//                GitHubPRTriggerMode triggerMode = trigger.getTriggerMode();
//
//                switch (triggerMode) {
//                    case HEAVY_HOOKS_CRON:
//                    case HEAVY_HOOKS: {
//                        LOGGER.debug("Queued check for {} (Branch {}) after heavy hook", job.getName(),
//                                ref.getBranchName());
//                        trigger.queueRun(job, ref.getBranchName());
//                        break;
//                    }
//                    case LIGHT_HOOKS: {
//                        LOGGER.warn("Unsupported LIGHT_HOOKS trigger mode");
//                        break;
//                    }
//                    default: {
//                        break;
//                    }
//                }
//            }

        } catch (Exception e) {
            LOGGER.error("Can't process {} hook", event, e);
        }
    }
}
