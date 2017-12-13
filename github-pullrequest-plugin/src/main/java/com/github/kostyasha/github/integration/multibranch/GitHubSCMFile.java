package com.github.kostyasha.github.integration.multibranch;

import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMRevision;
import org.kohsuke.github.GHRepository;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubSCMFile extends SCMFile {
    public static final String REFS = "refs/";

    private final GitHubSCMFileSystem gitHubSCMFileSystem;

    public GitHubSCMFile(GitHubSCMFileSystem gitHubSCMFileSystem) {
        super();
        this.gitHubSCMFileSystem = gitHubSCMFileSystem;
    }

    // for parent
    public GitHubSCMFile(GitHubSCMFileSystem gitHubSCMFileSystem, GitHubSCMFile gitHubSCMFile, String child) {
        super(gitHubSCMFile, child);
        this.gitHubSCMFileSystem = gitHubSCMFileSystem;
    }

    public GitHubSCMFile(@Nonnull GitHubSCMFile parent, String name) {
        super(parent, name);
        this.gitHubSCMFileSystem = parent.gitHubSCMFileSystem;
    }

    @Nonnull
    @Override
    protected SCMFile newChild(@Nonnull String child, boolean assumeIsDirectory) {
        return new GitHubSCMFile(this, child);
    }

    @Nonnull
    @Override
    public Iterable<SCMFile> children() throws IOException, InterruptedException {
        return null;
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        return 0;
    }

    @Nonnull
    @Override
    protected Type type() throws IOException, InterruptedException {
        return Type.REGULAR_FILE;
    }

    @Nonnull
    @Override
    public InputStream content() throws IOException, InterruptedException {
        SCMRevision revision = gitHubSCMFileSystem.getRevision();
        GitHubSCMRevision gitHubSCMRevision = (GitHubSCMRevision) revision;
        String hash = gitHubSCMRevision.getHash();

        GHRepository remoteRepo = gitHubSCMFileSystem.getRemoteRepo();
        return remoteRepo.getFileContent(getPath(), hash).read();
    }

}
