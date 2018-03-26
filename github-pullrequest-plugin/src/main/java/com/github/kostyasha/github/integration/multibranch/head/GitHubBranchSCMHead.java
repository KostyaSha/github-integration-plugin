package com.github.kostyasha.github.integration.multibranch.head;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRepository;

public class GitHubBranchSCMHead extends GitHubSCMHead {
    private static final long serialVersionUID = 1L;

    public GitHubBranchSCMHead(@Nonnull String name, String sourceId) {
        super(name, sourceId);
    }

    @Override
    public String getPronoun() {
        return "Branch " + getName();
    }

    @Override
    public String getHeadSha(GHRepository remoteRepo) throws IOException {
        GHBranch branch = remoteRepo.getBranch(getName());
        if (branch == null) {
            throw new IOException("No branch " + getName() + " in " + remoteRepo.getFullName());
        }
        return branch.getSHA1();
    }
}
