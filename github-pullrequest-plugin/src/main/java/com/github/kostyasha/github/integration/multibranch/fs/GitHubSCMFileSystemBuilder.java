package com.github.kostyasha.github.integration.multibranch.fs;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.head.GitHubSCMHead;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
import hudson.Extension;
import hudson.model.Item;
import hudson.scm.SCM;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import org.kohsuke.github.GHRepository;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubSCMFileSystemBuilder extends SCMFileSystem.Builder {
    @Override
    public boolean supports(SCMSource source) {
        return source instanceof GitHubSCMSource;
    }

    @Override
    public SCMFileSystem build(@NonNull SCMSource source, @NonNull SCMHead head, @CheckForNull SCMRevision rev)
            throws IOException, InterruptedException {
        if (source instanceof GitHubSCMSource) {
            GitHubSCMSource ghSCMSource = (GitHubSCMSource) source;
            GHRepository remoteRepo = ghSCMSource.getRemoteRepo();
            return new GitHubSCMFileSystem(remoteRepo, (GitHubSCMHead<?>) head, (GitHubSCMRevision) rev);
        }
        return null;
    }

    @Override
    public boolean supports(SCM source) {
        return false;
    }

    @Override
    public SCMFileSystem build(@NonNull Item owner, @NonNull SCM scm, @CheckForNull SCMRevision rev)
            throws IOException, InterruptedException {
        // I suppose we could inspect GitSCM for branch configuration
        return null;
    }

}
