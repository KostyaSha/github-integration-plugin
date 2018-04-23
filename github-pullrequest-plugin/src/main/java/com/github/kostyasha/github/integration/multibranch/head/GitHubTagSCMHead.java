package com.github.kostyasha.github.integration.multibranch.head;

import com.github.kostyasha.github.integration.tag.GitHubTag;
import com.github.kostyasha.github.integration.tag.GitHubTagCause;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTag;

import javax.annotation.Nonnull;
import java.io.IOException;

public class GitHubTagSCMHead extends GitHubSCMHead<GitHubTagCause> {
    private static final long serialVersionUID = 1L;

    public GitHubTagSCMHead(@Nonnull String name, String sourceId) {
        super(name, sourceId);
    }

    @Override
    public String getPronoun() {
        return "Tag " + getName();
    }

    @Override
    public String fetchHeadSha(GHRepository remoteRepo) throws IOException {
        GHTag tag = GitHubTag.findRemoteTag(remoteRepo, getName());
        if (tag == null) {
            throw new IOException("No tag " + getName() + " in " + remoteRepo.getFullName());
        }
        return tag.getCommit().getSHA1();
    }

    public String getHeadSha(GitHubTagCause cause) {
        return cause.getCommitSha();
    }
}
