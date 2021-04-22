package com.github.kostyasha.github.integration.multibranch.fs;

import com.github.kostyasha.github.integration.multibranch.fs.TreeCache.Entry;
import jenkins.scm.api.SCMFile;

import edu.umd.cs.findbugs.annotations.NonNull;
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

    public GitHubSCMFile(@NonNull GitHubSCMFile parent, String name) {
        super(parent, name);
        this.fs = parent.fs;
    }

    private Entry getEntry() throws IOException {
        if (entry == null) {
            synchronized (this) {
                if (entry == null) {
                    entry = fs.tree().entry(getPath());
                }
            }
        }
        return entry;
    }

    private SCMFile newChild(@NonNull String child) {
        return new GitHubSCMFile(this, child);
    }

    @NonNull
    @Override
    protected SCMFile newChild(@NonNull String child, boolean assumeIsDirectory) {
        return newChild(child);
    }

    @NonNull
    @Override
    public Iterable<SCMFile> children() throws IOException, InterruptedException {
        return getEntry().getSubEntryNames().stream()
                .map(this::newChild)
                .collect(Collectors.toList());
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        return fs.lastModified();
    }

    @NonNull
    @Override
    protected Type type() throws IOException, InterruptedException {
        return getEntry().getType();
    }

    @NonNull
    @Override
    public InputStream content() throws IOException, InterruptedException {
        return fs.tree().content(getEntry());
    }

}
