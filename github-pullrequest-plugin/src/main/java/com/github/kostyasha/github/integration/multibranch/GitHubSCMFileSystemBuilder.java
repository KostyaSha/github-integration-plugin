package com.github.kostyasha.github.integration.multibranch;

import hudson.model.Item;
import hudson.scm.SCM;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubSCMFileSystemBuilder  extends SCMFileSystem.Builder{
    @Override
    public boolean supports(SCM source) {
        return false;
    }

    @Override
    public boolean supports(SCMSource source) {
        return source instanceof GitHubSCMSource;
    }

    @Override
    public SCMFileSystem build(@Nonnull Item owner, @Nonnull SCM scm, @CheckForNull SCMRevision rev)
            throws IOException, InterruptedException {
        return null;
    }
}
