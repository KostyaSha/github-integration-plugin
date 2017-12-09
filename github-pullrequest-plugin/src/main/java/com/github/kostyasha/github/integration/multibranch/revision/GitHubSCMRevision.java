package com.github.kostyasha.github.integration.multibranch.revision;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubSCMRevision extends AbstractGitSCMSource.SCMRevisionImpl {
    private final boolean superEquals;
    transient GitHubCause cause;


    public GitHubSCMRevision(SCMHead head, String hash, boolean superEquals, GitHubCause cause) {
        super(head, hash);
        this.superEquals = superEquals;
        this.cause = cause;
    }

    public GitHubCause getCause() {
        return cause;
    }

    public GitHubSCMRevision setCause(GitHubCause cause) {
        this.cause = cause;
        return this;
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
