package com.github.kostyasha.github.integration.multibranch;

import com.github.kostyasha.github.integration.multibranch.head.GitHubSCMHead;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.scm.SCM;
import jenkins.branch.MultiBranchProject;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import org.kohsuke.github.GHRepository;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubSCMFileSystemBuilder extends SCMFileSystem.Builder {
    @Override
    public boolean supports(SCM source) {
        return true;
    }

    @Override
    public boolean supports(SCMSource source) {
        return source instanceof GitHubSCMSource;
    }

    @Override
    public SCMFileSystem build(@Nonnull Item owner, @Nonnull SCM scm, @CheckForNull SCMRevision rev)
            throws IOException, InterruptedException {
        SCMHead head = rev.getHead();
        GitHubSCMRevision ghRev = (GitHubSCMRevision) rev;
        GitHubSCMHead ghHead = (GitHubSCMHead) head;

        String sourceId = ghHead.getSourceId();
        MultiBranchProject multiBranchProject = (MultiBranchProject) owner;
        SCMSource scmSource = multiBranchProject.getSCMSource(sourceId);
        if (scmSource instanceof GitHubSCMSource) {
            GitHubSCMSource ghSCMSource = (GitHubSCMSource) scmSource;
            GHRepository remoteRepo = ghSCMSource.getRemoteRepo();
            return new GitHubSCMFileSystem(ghRev, remoteRepo);
        }

        return null;
    }
}
