package com.github.kostyasha.github.integration.branch.webhook;

/**
 * Main info from webhook event.
 *
 * @author Kanstantsin Shautsou
 */
public class RefInfo {
    private String repo;
    private String refType;
    private String refName;

    public RefInfo(String repo, String refType, String refName) {
        this.repo = repo;
        this.refType = refType;
        this.refName = refName;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getRefType() {
        return refType;
    }

    public void setRefType(String refType) {
        this.refType = refType;
    }

    public String getRefName() {
        return refName;
    }

    public void setRefName(String refName) {
        this.refName = refName;
    }
}
