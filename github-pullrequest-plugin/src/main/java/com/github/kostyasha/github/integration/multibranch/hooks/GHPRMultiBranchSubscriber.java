package com.github.kostyasha.github.integration.multibranch.hooks;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubHandler;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubPRHandler;
import hudson.Extension;
import hudson.model.Item;
import jenkins.scm.api.SCMEvent;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.jenkinsci.plugins.github.pullrequest.webhook.AbstractGHPullRequestSubsriber;
import org.jenkinsci.plugins.github.pullrequest.webhook.PullRequestInfo;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * @author Kanstantsin Shautsou
 */
@SuppressWarnings("unused")
@Extension
public class GHPRMultiBranchSubscriber extends AbstractGHPullRequestSubsriber {
    public static final Logger LOG = LoggerFactory.getLogger(GHPRMultiBranchSubscriber.class);

    @Override
    protected boolean isApplicable(@Nullable Item item) {
        if (item instanceof SCMSourceOwner) {
            SCMSourceOwner scmSourceOwner = (SCMSourceOwner) item;
            for (SCMSource source : scmSourceOwner.getSCMSources()) {
                if (source instanceof GitHubSCMSource) {
                    GitHubSCMSource gitHubSCMSource = (GitHubSCMSource) source;
                    for (GitHubHandler hubHandler : gitHubSCMSource.getHandlers()) {
                        if (hubHandler instanceof GitHubPRHandler) {
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
            GitHub gh = GitHub.offline();

            PullRequestInfo ref = extractPullRequestInfo(event.getGHEvent(), event.getPayload(), gh);
            SCMHeadEvent.fireNow(new GitHubPullRequestScmHeadEvent(
                    SCMEvent.Type.UPDATED,
                    event.getTimestamp(),
                    ref,
                    ref.getRepo())
            );

        } catch (Exception e) {
            LOG.error("Can't process {} hook", event, e);
        }
    }
}
