package com.github.kostyasha.github.integration.multibranch.hooks;

import com.github.kostyasha.github.integration.multibranch.head.GitHubPRSCMHead;

import hudson.scm.SCM;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.github.pullrequest.webhook.PullRequestInfo;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPullRequestScmHeadEvent extends GitHubScmHeadEvent<PullRequestInfo> {
    public GitHubPullRequestScmHeadEvent(@Nonnull Type type, long timestamp, @Nonnull PullRequestInfo payload, @CheckForNull String origin) {
        super(type, timestamp, payload, origin);
    }

    @Nonnull
    @Override
    protected String getSourceRepo() {
        return getPayload().getRepo();
    }

    @Nonnull
    @Override
    public Map<SCMHead, SCMRevision> heads(@Nonnull SCMSource source) {
        if (!isMatch(source)) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap(new GitHubPRSCMHead(getPayload().getNum(), getPayload().getTarget(), source.getId()), null);
    }

    @Override
    public boolean isMatch(@Nonnull SCM scm) {
        return false;
    }
}
