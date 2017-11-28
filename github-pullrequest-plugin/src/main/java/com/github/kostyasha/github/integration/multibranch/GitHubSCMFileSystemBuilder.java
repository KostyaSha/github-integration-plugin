package com.github.kostyasha.github.integration.multibranch;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Item;
import hudson.scm.SCM;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;

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
    public SCMFileSystem build(@NonNull Item owner, @NonNull SCM scm, @CheckForNull SCMRevision rev)
            throws IOException, InterruptedException {
        return null;
    }
}
