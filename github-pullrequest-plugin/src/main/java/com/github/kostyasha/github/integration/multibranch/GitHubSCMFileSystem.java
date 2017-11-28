package com.github.kostyasha.github.integration.multibranch;

import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMRevision;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubSCMFileSystem extends SCMFileSystem {
    /**
     * Constructor.
     *
     * @param rev the revision.
     */
    protected GitHubSCMFileSystem(@CheckForNull SCMRevision rev) {
        super(rev);
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        return 0;
    }

    @Nonnull
    @Override
    public SCMFile getRoot() {
        return null;
    }
}
