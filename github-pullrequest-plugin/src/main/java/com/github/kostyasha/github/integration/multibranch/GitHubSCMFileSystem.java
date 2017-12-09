package com.github.kostyasha.github.integration.multibranch;

import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMRevision;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRepository;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubSCMFileSystem extends SCMFileSystem {
    private final GHRepository remoteRepo;

    /**
     * Constructor.
     *
     * @param rev the revision.
     * @param remoteRepo
     */
    protected GitHubSCMFileSystem(@CheckForNull GitHubSCMRevision rev, @Nonnull GHRepository remoteRepo) {
        super(rev);
        this.remoteRepo = remoteRepo;
    }

    @Nonnull
    public GHRepository getRemoteRepo() {
        return remoteRepo;
    }

    @Override
    public SCMRevision getRevision() {
        return (GitHubSCMRevision) super.getRevision();
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        return 0L;
    }

    @Nonnull
    @Override
    public SCMFile getRoot() {
        return new GitHubSCMFile(this, "/");
    }
}
