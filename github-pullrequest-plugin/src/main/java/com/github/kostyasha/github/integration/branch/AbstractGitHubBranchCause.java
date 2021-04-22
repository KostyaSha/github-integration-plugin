package com.github.kostyasha.github.integration.branch;

import com.github.kostyasha.github.integration.generic.GitHubCause;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class AbstractGitHubBranchCause<T extends AbstractGitHubBranchCause<T>> extends GitHubCause<T> {

    /**
     * null for deleted branch/tag
     */
    @CheckForNull
    private final String commitSha;

    @CheckForNull
    private final String fullRef;

    protected AbstractGitHubBranchCause(String commitSha, String fullRef) {
        this.commitSha = commitSha;
        this.fullRef = fullRef;
    }

    /**
     * Copy constructor
     */
    protected AbstractGitHubBranchCause(AbstractGitHubBranchCause<T> cause) {
        this(cause.getCommitSha(), cause.getFullRef());
        withGitUrl(cause.getGitUrl());
        withSshUrl(cause.getSshUrl());
        withHtmlUrl(cause.getHtmlUrl());
        withPollingLog(cause.getPollingLog());
        withReason(cause.getReason());
        withSkip(cause.isSkip());
        withTitle(cause.getTitle());
    }

    @CheckForNull
    public String getCommitSha() {
        return commitSha;
    }

    @CheckForNull
    public String getFullRef() {
        return fullRef;
    }

}
