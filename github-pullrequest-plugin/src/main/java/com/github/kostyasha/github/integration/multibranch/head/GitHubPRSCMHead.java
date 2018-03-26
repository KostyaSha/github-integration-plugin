package com.github.kostyasha.github.integration.multibranch.head;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.IOException;

import javax.annotation.Nonnull;

public class GitHubPRSCMHead extends GitHubSCMHead {
    private static final long serialVersionUID = 1L;

    private final int pr;
    private final String targetBranch;

    public GitHubPRSCMHead(@Nonnull GitHubPRCause prCause, String sourceId) {
        this(prCause.getNumber(), prCause.getTargetBranch(), sourceId);
    }

    public GitHubPRSCMHead(@Nonnull Integer pr, @Nonnull String targetBranch, String sourceId) {
        super("pr-" + Integer.toString(pr), sourceId);
        this.pr = pr;
        this.targetBranch = targetBranch;
    }

    public int getPr() {
        return pr;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    @Override
    public String getPronoun() {
        return "PR#" + pr;
    }

    @Override
    public String getHeadSha(GHRepository remoteRepo) throws IOException {
        GHPullRequest pullRequest = remoteRepo.getPullRequest(pr);
        if (pullRequest == null) {
            throw new IOException("No PR " + pr + " in " + remoteRepo.getFullName());
        }
        return pullRequest.getHead().getSha();
    }
}
