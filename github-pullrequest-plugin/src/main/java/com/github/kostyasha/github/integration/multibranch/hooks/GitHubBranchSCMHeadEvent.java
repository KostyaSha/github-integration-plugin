package com.github.kostyasha.github.integration.multibranch.hooks;

import com.github.kostyasha.github.integration.branch.webhook.BranchInfo;
import com.github.kostyasha.github.integration.multibranch.head.GitHubBranchSCMHead;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.Map;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchSCMHeadEvent extends GitHubScmHeadEvent<BranchInfo> {
    public GitHubBranchSCMHeadEvent(@NonNull Type type, long timestamp, @NonNull BranchInfo payload, @CheckForNull String origin) {
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
        return Collections.singletonMap(new GitHubBranchSCMHead(getPayload().getBranchName(), source.getId()), null);
    }

}
