package com.github.kostyasha.github.integration.multibranch.head;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

public class GitHubPRSCMHead extends GitHubSCMHead<GitHubPRCause> {
    private static final long serialVersionUID = 1L;

    private final int prNumber;
    private final String targetBranch;

    public GitHubPRSCMHead(@NonNull GitHubPRCause prCause, String sourceId) {
        this(prCause.getNumber(), prCause.getTargetBranch(), sourceId);
    }

    public GitHubPRSCMHead(@NonNull Integer prNumber, @NonNull String targetBranch, String sourceId) {
        super("pr-" + Integer.toString(prNumber), sourceId);
        this.prNumber = prNumber;
        this.targetBranch = targetBranch;
    }

    public int getPrNumber() {
        return prNumber;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    @Override
    public String getPronoun() {
        return "PR#" + prNumber;
    }

    @Override
    public String fetchHeadSha(GHRepository remoteRepo) throws IOException {
        GHPullRequest pullRequest = remoteRepo.getPullRequest(prNumber);
        if (pullRequest == null) {
            throw new IOException("No PR " + prNumber + " in " + remoteRepo.getFullName());
        }
        return pullRequest.getHead().getSha();
    }

    @Override
    public String getHeadSha(GitHubPRCause cause) {
        return cause.getHeadSha();
    }
}
