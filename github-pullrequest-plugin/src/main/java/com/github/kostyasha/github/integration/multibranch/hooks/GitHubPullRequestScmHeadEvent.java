package com.github.kostyasha.github.integration.multibranch.hooks;

import com.github.kostyasha.github.integration.multibranch.head.GitHubPRSCMHead;
import hudson.scm.SCM;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.github.pullrequest.webhook.PullRequestInfo;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.Map;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPullRequestScmHeadEvent extends GitHubScmHeadEvent<PullRequestInfo> {
    public GitHubPullRequestScmHeadEvent(@NonNull Type type, long timestamp, @NonNull PullRequestInfo payload,
                                         @CheckForNull String origin) {
        super(type, timestamp, payload, origin);
    }

    @NonNull
    @Override
    protected String getSourceRepo() {
        return getPayload().getRepo();
    }

    @NonNull
    @Override
    public Map<SCMHead, SCMRevision> heads(@NonNull SCMSource source) {
        if (!isMatch(source)) {
            return Collections.emptyMap();
        }

        return Collections.singletonMap(
                new GitHubPRSCMHead(getPayload().getNum(), getPayload().getTarget(), source.getId()), null
        );
    }

    @Override
    public boolean isMatch(@NonNull SCM scm) {
        return false;
    }
}
