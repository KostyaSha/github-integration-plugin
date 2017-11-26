package com.github.kostyasha.github.integration.multibranch.revision;

import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubSCMRevision extends AbstractGitSCMSource.SCMRevisionImpl {
    private final boolean superEquals;

    public GitHubSCMRevision(SCMHead head, String hash, boolean superEquals) {
        super(head, hash);
        this.superEquals = superEquals;
    }

    @Override
    public boolean equals(Object obj) {
        if (superEquals) {
            return super.equals(obj);
        }

        //we control when to run outside of multibranch logic
        return false;
    }

}
