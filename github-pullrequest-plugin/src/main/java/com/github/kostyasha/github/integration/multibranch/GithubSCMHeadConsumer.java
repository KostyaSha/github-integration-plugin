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

import hudson.model.TaskListener;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMProbe;
import jenkins.scm.api.SCMProbeStat;
import jenkins.scm.api.SCMSourceCriteria;

public class GithubSCMHeadConsumer implements SCMHeadConsumer {

  private final GitHubSCMSource source;
  private final SCMHeadObserver observer;
  private final SCMSourceCriteria criteria;
  private final TaskListener listener;

  public GithubSCMHeadConsumer(GitHubSCMSource source, SCMHeadObserver observer, SCMSourceCriteria criteria, TaskListener listener) {
    this.source = source;
    this.observer = observer;
    this.criteria = criteria;
    this.listener = listener;
  }

  @Override
  public void accept(GitHubSCMHead head, GitHubSCMRevision revision) throws IOException, InterruptedException {
    listener.getLogger().println("Checking " + head.getPronoun());
    if (criteria != null) {
      if (!criteria.isHead(getProbe(head, revision), listener)) {
        listener.getLogger().println("  Didn't meet criteria");
        return;
      } else {
        listener.getLogger().println("  Met criteria");
      }
    }
    observer.observe(head, revision);
  }

  private SCMProbe getProbe(GitHubSCMHead head, GitHubSCMRevision revision) {
    return new GithubProbe(head, revision);
  }

  private class GithubProbe extends SCMProbe {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String hash;
    private volatile GHRepository repo;
    private volatile TreeCache tree;

    public GithubProbe(GitHubSCMHead head, GitHubSCMRevision revision) {
      this.name = head.getName();
      this.hash = revision.getHash();
    }

    @Override
    public String name() {
      return name;
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
        GHCommit commit = repo().getCommit(hash);
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
          tree = repo().getTree(hash);
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


  }

  private static class Entry {
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

    private static final Entry NONE = new Entry(null, false);
    private static final Entry FILE = new Entry(null);

  }


}
