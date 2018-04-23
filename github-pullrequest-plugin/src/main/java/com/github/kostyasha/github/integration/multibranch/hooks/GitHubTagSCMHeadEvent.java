package com.github.kostyasha.github.integration.multibranch.hooks;

import com.github.kostyasha.github.integration.branch.webhook.BranchInfo;
import com.github.kostyasha.github.integration.multibranch.head.GitHubTagSCMHead;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubTagSCMHeadEvent extends GitHubScmHeadEvent<BranchInfo> {
    public GitHubTagSCMHeadEvent(@Nonnull Type type, long timestamp, @Nonnull BranchInfo payload, @CheckForNull String origin) {
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
        return Collections.singletonMap(new GitHubTagSCMHead(getPayload().getBranchName(), source.getId()), null);
    }

}
