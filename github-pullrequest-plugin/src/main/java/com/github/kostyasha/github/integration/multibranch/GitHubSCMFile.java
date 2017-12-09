package com.github.kostyasha.github.integration.multibranch;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMFile;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubSCMFile extends SCMFile {
    private final GitHubSCMFileSystem gitHubSCMFileSystem;
    private final String path;

    public GitHubSCMFile(GitHubSCMFileSystem gitHubSCMFileSystem, String path) {

        this.gitHubSCMFileSystem = gitHubSCMFileSystem;
        this.path = path;
    }

    @Nonnull
    @Override
    protected SCMFile newChild(@Nonnull String name, boolean assumeIsDirectory) {
        return null;
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
        return null;
    }

    @Nonnull
    @Override
    public InputStream content() throws IOException, InterruptedException {
        return null;
    }
}
