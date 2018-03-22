package com.github.kostyasha.github.integration.multibranch;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;

import com.github.kostyasha.github.integration.multibranch.head.GitHubSCMHead;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;

import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMProbe;
import jenkins.scm.api.SCMProbeStat;

public class GitHubSCMProbe extends SCMProbe {
    private static final long serialVersionUID = 1L;

    private final GitHubSCMSource source;
    private final GitHubSCMHead head;
    private final GitHubSCMRevision revision;
    private volatile GHRepository repo;
    private volatile TreeCache tree;

    public GitHubSCMProbe(GitHubSCMSource source, GitHubSCMHead head, GitHubSCMRevision revision) {
        this.source = source;
        this.head = head;
        this.revision = revision;
    }

    public GitHubSCMHead getHead() {
        return head;
    }

    public GitHubSCMRevision getRevision() {
        return revision;
    }

    @Override
    public String name() {
        return head.getName();
    }

    @Override
    public void close() throws IOException {}

    @Override
    public SCMProbeStat stat(String path) throws IOException {
        Entry e = tree().entry(path);
        return SCMProbeStat.fromType(e.type);
    }

    @Override
    public long lastModified() {
        try {
            GHCommit commit = repo().getCommit(revision.getHash());
            return commit.getCommitDate().getTime();
        } catch (IOException e) {
            return 0L;
        }
    }

    private GHRepository repo() {
        if (repo == null) {
            synchronized (this) {
                if (repo == null) {
                    repo = source.getRepoProvider().getGHRepository(source);
                }
            }
        }
        return repo;
    }


    private TreeCache tree() throws IOException {
        if (tree == null) {
            synchronized (this) {
                if (tree == null) {
                    tree = new TreeCache();
                }
            }
        }
        return tree;
    }

    private class TreeCache {
        Map<String, Entry> entries = new HashMap<>();

        Entry entry(String path) throws IOException {
            while (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            Entry e = entries.get(path);
            if (e != null) {
                return e;
            }

            // root
            if (path.equals("")) {
                e = new Entry(null, false);
                entries.put("", e);
                return e;
            }

            Entry parent;
            int idx = path.lastIndexOf('/');
            if (idx == -1) {
                parent = entry("");
            } else {
                parent = entry(path.substring(0, idx));
            }

            if (parent == Entry.NONE) {
                entries.put(path, parent);
                return parent;
            }

            if (parent.file) {
                entries.put(path, Entry.NONE);
                return Entry.NONE;
            }

            // parent is a dir, fetch its tree
            if (!parent.processed) {
                fillSubEntries(parent);
                e = entries.get(path);
            }

            if (e == null) {
                e = Entry.NONE;
            }
            entries.put(path, e);
            return e;
        }

        private void fillSubEntries(Entry e) throws IOException {
            GHTree tree;
            if (e.entry == null) {
                tree = repo().getTree(revision.getHash());
            } else {
                tree = e.entry.asTree();
            }

            for (GHTreeEntry entry : tree.getTree()) {
                String newPath = entry.getPath();
                Entry ee = "tree".equals(entry.getType()) ? new Entry(entry) : Entry.FILE;
                entries.put(newPath, ee);
            }
            e.processed = true;
        }
    }

    static class Entry {
        final GHTreeEntry entry;
        final boolean file;
        final SCMFile.Type type;
        boolean processed;

        Entry(GHTreeEntry entry) {
            this(entry, entry == null);
        }

        Entry(GHTreeEntry entry, boolean file) {
            this.entry = entry;
            this.file = file;

            if (entry != null) {
                type = SCMFile.Type.DIRECTORY;
            } else if (file) {
                type = SCMFile.Type.REGULAR_FILE;
            } else {
                type = SCMFile.Type.NONEXISTENT;
            }
        }

        static final Entry NONE = new Entry(null, false);
        static final Entry FILE = new Entry(null);

    }

}
