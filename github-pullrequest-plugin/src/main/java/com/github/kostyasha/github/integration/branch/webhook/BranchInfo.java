package com.github.kostyasha.github.integration.branch.webhook;

/**
 * Main info from webhook event.
 *
 * @author Kanstantsin Shautsou
 * @see GHBranchSubscriber
 */
public class BranchInfo {
    private String repo;
    private String branchName;
    private String fullRef;
    private boolean tag;

    public BranchInfo(String repo, String branchName, String fullRef, boolean tag) {
        this.repo = repo;
        this.branchName = branchName;
        this.fullRef = fullRef;
        this.tag = tag;
    }

    public boolean isTag() {
        return tag;
    }

    public String getRepo() {
        return repo;
    }

    public BranchInfo withRepo(String repo) {
        this.repo = repo;
        return this;
    }

    public String getBranchName() {
        return branchName;
    }

    public BranchInfo withBranchName(String branchName) {
        this.branchName = branchName;
        return this;
    }

    public String getFullRef() {
        return fullRef;
    }

    public BranchInfo withFullRef(String fullRef) {
        this.fullRef = fullRef;
        return this;
    }
}
