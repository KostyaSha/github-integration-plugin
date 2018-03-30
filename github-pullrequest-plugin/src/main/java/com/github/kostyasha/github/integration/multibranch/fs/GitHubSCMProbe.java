package com.github.kostyasha.github.integration.multibranch.fs;

import java.io.IOException;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.head.GitHubSCMHead;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;

import jenkins.scm.api.SCMProbe;
import jenkins.scm.api.SCMProbeStat;

public class GitHubSCMProbe extends SCMProbe {
    private static final long serialVersionUID = 1L;

    private final GitHubSCMSource source;
    private final GitHubSCMHead<?> head;
    private final GitHubSCMRevision revision;
    private volatile GHRepository repo;
    private volatile GHCommit commit;
    private volatile TreeCache tree;

    public GitHubSCMProbe(GitHubSCMSource source, GitHubSCMHead<?> head, GitHubSCMRevision revision) {
        this.source = source;
        this.head = head;
        this.revision = revision;
    }

    public GitHubSCMHead<?> getHead() {
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
        TreeCache.Entry e = tree().entry(path);
        return SCMProbeStat.fromType(e.type);
    }

    @Override
    public long lastModified() {
        Object rd = revision.getRemoteData();
        if (rd instanceof GHPullRequest) {
            GHPullRequest pr = (GHPullRequest) rd;
            if (pr != null) {
                try {
                    return pr.getUpdatedAt().getTime();
                } catch (IOException e) {
                    // never thrown
                }
            }
        }
        try {
            return commit().getCommitDate().getTime();
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

    private GHCommit commit() throws IOException {
        if (commit == null) {
            synchronized (commit) {
                if (commit == null) {
                    commit = repo().getCommit(revision.getHash());
                }
            }
        }
        return commit;
    }

    private TreeCache tree() {
        if (tree == null) {
            synchronized (this) {
                if (tree == null) {
                    tree = TreeCache.get(repo(), revision.getHash());
                }
            }
        }
        return tree;
    }

}
