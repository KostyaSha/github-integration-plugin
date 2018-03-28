package com.github.kostyasha.github.integration.multibranch.head;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRepository;

import com.github.kostyasha.github.integration.branch.GitHubBranchCause;

public class GitHubBranchSCMHead extends GitHubSCMHead<GitHubBranchCause> {
    private static final long serialVersionUID = 1L;

    public GitHubBranchSCMHead(@Nonnull String name, String sourceId) {
        super(name, sourceId);
    }

    @Override
    public String getPronoun() {
        return "Branch " + getName();
    }

    @Override
    public String fetchHeadSha(GHRepository remoteRepo) throws IOException {
        GHBranch branch = remoteRepo.getBranch(getName());
        if (branch == null) {
            throw new IOException("No branch " + getName() + " in " + remoteRepo.getFullName());
        }
        return branch.getSHA1();
    }

    @Override
    public String getHeadSha(GitHubBranchCause cause) {
        return cause.getCommitSha();
    }
}
