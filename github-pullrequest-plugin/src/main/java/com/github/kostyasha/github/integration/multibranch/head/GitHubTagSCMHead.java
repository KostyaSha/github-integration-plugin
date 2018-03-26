package com.github.kostyasha.github.integration.multibranch.head;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTag;

import com.github.kostyasha.github.integration.tag.GitHubTag;

public class GitHubTagSCMHead extends GitHubSCMHead {
    private static final long serialVersionUID = 1L;

    public GitHubTagSCMHead(@Nonnull String name, String sourceId) {
        super(name, sourceId);
    }

    @Override
    public String getPronoun() {
        return "Tag " + getName();
    }

    @Override
    public String getHeadSha(GHRepository remoteRepo) throws IOException {
        GHTag tag = GitHubTag.findRemoteTag(remoteRepo, getName());
        if (tag == null) {
            throw new IOException("No tag " + getName() + " in " + remoteRepo.getFullName());
        }
        return tag.getCommit().getSHA1();
    }
}
