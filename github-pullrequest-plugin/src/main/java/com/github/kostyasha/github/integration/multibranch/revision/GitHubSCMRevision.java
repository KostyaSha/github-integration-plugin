package com.github.kostyasha.github.integration.multibranch.revision;

import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubSCMRevision extends AbstractGitSCMSource.SCMRevisionImpl {
    public GitHubSCMRevision(SCMHead head, String hash) {
        super(head, hash);
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

//    @Override
//    public int hashCode() {
//        return 0;
//    }
}
