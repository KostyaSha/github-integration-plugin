package com.github.kostyasha.github.integration.multibranch.fs;

import jenkins.scm.api.SCMFile;
import org.apache.commons.io.IOUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

public class TreeCache {
    private static final long CONTENT_THRESHOLD = 1024 * 1024; // 1MB max

    private final GHRepository repo;
    private final String revision;
    private final Map<String, Entry> entries = new HashMap<>();

    private TreeCache(GHRepository repo, String revision) {
        this.repo = repo;
        this.revision = revision;
    }

    public Entry rootEntry() throws IOException {
        return entry("");
    }

    public Entry entry(String path) throws IOException {
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        Entry e = entries.get(path);
        if (e != null) {
            return e;
        }

        // root
        if (path.equals("")) {
            e = new Entry("", null, false);
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

    public InputStream content(Entry entry) throws IOException {
        return entry.load(repo, revision);
    }

    private void fillSubEntries(Entry e) throws IOException {
        GHTree tree;
        String dir;
        if (e.entry == null) {
            tree = repo.getTree(revision);
            dir = "";
        } else {
            tree = e.entry.asTree();
            dir = e.path + "/";
        }

        List<String> subEntries = new ArrayList<>();
        for (GHTreeEntry sub : tree.getTree()) {
            String newPath = dir + sub.getPath();
            Entry ee = "tree".equals(sub.getType()) ? new Entry(newPath, sub) : new Entry(newPath, null);
            subEntries.add(sub.getPath());
            entries.put(newPath, ee);
        }
        e.subEntries = subEntries;
        e.processed = true;
    }

    public static class Entry {
        private static final Entry NONE = new Entry(null, null, false);
        private final String path;
        private final GHTreeEntry entry;
        private final boolean file;
        private final SCMFile.Type type;
        private List<String> subEntries;
        private boolean processed;

        private volatile byte[] cachedContent;

        public SCMFile.Type getType() {
            return type;
        }

        Entry(String path, GHTreeEntry entry) {
            this(path, entry, entry == null);
        }

        InputStream load(GHRepository repo, String revision) throws IOException {
            if (isNull(cachedContent)) {
                synchronized (this) {
                    if (cachedContent == null) {
                        GHContent content = repo.getFileContent(path, revision);
                        long size = content.getSize();
                        if (size > CONTENT_THRESHOLD) {
                            // don't cache
                            return content.read();
                        }

                        byte[] buf = new byte[(int) size];
                        try (InputStream in = content.read()) {
                            IOUtils.readFully(in, buf);
                        }
                        cachedContent = buf;
                    }
                }
            }
            return new ByteArrayInputStream(cachedContent);
        }

        Entry(String path, GHTreeEntry entry, boolean file) {
            this.path = path;
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

        public List<String> getSubEntryNames() {
            return subEntries;
        }
    }

    public static TreeCache get(GHRepository repo, String revision) {
        Context ctx = CONTEXT.get();
        if (ctx != null) {
            return ctx.get(repo, revision);
        }
        return new TreeCache(repo, revision);
    }

    // starts a context to cache all TreeCache instances to allow reusing them
    public static Context createContext() {
        return new Context();
    }

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    public static class Context implements AutoCloseable {
        private final boolean opened;
        private final Map<String, TreeCache> cache;

        public Context() {
            if (CONTEXT.get() != null) {
                opened = false;
            } else {
                opened = true;
                CONTEXT.set(this);
            }
            cache = new HashMap<>();
        }

        public TreeCache get(GHRepository repo, String revision) {
            return cache.computeIfAbsent(key(repo, revision), k -> new TreeCache(repo, revision));
        }

        private String key(GHRepository repo, String revision) {
            return repo.getHtmlUrl() + "#" + revision;
        }

        @Override
        public void close() {
            if (opened) {
                cache.clear();
                CONTEXT.set(null);
            }
        }
    }
}
