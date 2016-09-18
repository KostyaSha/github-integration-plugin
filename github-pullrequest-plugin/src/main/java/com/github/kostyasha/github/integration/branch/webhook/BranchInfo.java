package com.github.kostyasha.github.integration.branch.webhook;

import javax.annotation.CheckForNull;

/**
 * Main info from webhook event.
 *
 * @author Kanstantsin Shautsou
 */
public class BranchInfo {
    private String repo;
    private String branchName;

    public BranchInfo(String repo, String branchName) {
        this.repo = repo;
        this.branchName = branchName;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

}
