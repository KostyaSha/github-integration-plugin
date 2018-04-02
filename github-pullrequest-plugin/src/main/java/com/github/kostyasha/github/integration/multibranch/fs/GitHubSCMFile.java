package com.github.kostyasha.github.integration.multibranch.fs;

import com.github.kostyasha.github.integration.multibranch.fs.TreeCache.Entry;
import jenkins.scm.api.SCMFile;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubSCMFile extends SCMFile {

    private final GitHubSCMFileSystem fs;
    private volatile Entry entry;

    public GitHubSCMFile(GitHubSCMFileSystem fs) {
        this.fs = fs;
    }

    public GitHubSCMFile(GitHubSCMFileSystem fs, GitHubSCMFile parent, String name) {
        super(parent, name);
        this.fs = fs;
    }

    public GitHubSCMFile(@Nonnull GitHubSCMFile parent, String name) {
        super(parent, name);
        this.fs = parent.fs;
    }

    private Entry entry() throws IOException {
        if (entry == null) {
            synchronized (this) {
                if (entry == null) {
                    entry = fs.tree().entry(getPath());
                }
            }
        }
        return entry;
    }

    private SCMFile newChild(@Nonnull String child) {
        return new GitHubSCMFile(this, child);
    }

    @Nonnull
    @Override
    protected SCMFile newChild(@Nonnull String child, boolean assumeIsDirectory) {
        return newChild(child);
    }

    @Nonnull
    @Override
    public Iterable<SCMFile> children() throws IOException, InterruptedException {
        return entry().getSubEntryNames().stream()
                .map(this::newChild)
                .collect(Collectors.toList());
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        return fs.lastModified();
    }

    @Nonnull
    @Override
    protected Type type() throws IOException, InterruptedException {
        return entry().type;
    }

    @Nonnull
    @Override
    public InputStream content() throws IOException, InterruptedException {
        return fs.tree().content(entry());
    }

}
