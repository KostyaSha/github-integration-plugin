package com.github.kostyasha.github.integration.multibranch.fs;

import com.github.kostyasha.github.integration.multibranch.head.GitHubSCMHead;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubSCMFileSystem extends SCMFileSystem {
    private final GHRepository remoteRepo;
    private final GHCommit commit;
    private volatile TreeCache tree;

    protected GitHubSCMFileSystem(@Nonnull GHRepository remoteRepo, GitHubSCMHead<?> head, GitHubSCMRevision rev) throws IOException {
        super(rev);
        this.remoteRepo = remoteRepo;
        this.commit = remoteRepo.getCommit(calculateHash(remoteRepo, head, rev));
    }

    private static String calculateHash(GHRepository remoteRepo, GitHubSCMHead<?> head, GitHubSCMRevision rev) throws IOException {
        if (rev != null) {
            return rev.getHash();
        }
        return head.fetchHeadSha(remoteRepo);
    }

    @Nonnull
    public String getCommitSha() {
        return commit.getSHA1();
    }

    @Nonnull
    public GHRepository getRemoteRepo() {
        return remoteRepo;
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        return commit.getCommitDate().getTime();
    }

    @Nonnull
    @Override
    public SCMFile getRoot() {
        return new GitHubSCMFile(this);
    }

    TreeCache tree() {
        if (tree == null) {
            synchronized (this) {
                if (tree == null) {
                    tree = TreeCache.get(remoteRepo, getCommitSha());
                }
            }
        }
        return tree;
    }
}
