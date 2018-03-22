package com.github.kostyasha.github.integration.multibranch.revision;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMHead;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubSCMRevision extends AbstractGitSCMSource.SCMRevisionImpl {
    private transient GitHubCause cause;

    public GitHubSCMRevision(SCMHead head, String hash, GitHubCause cause) {
        super(head, hash);
        this.cause = cause;
    }

    public GitHubCause getCause() {
        return cause;
    }

    public GitHubSCMRevision setCause(GitHubCause cause) {
        this.cause = cause;
        return this;
    }

}
