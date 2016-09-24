package com.github.kostyasha.github.integration.branch;

import hudson.Functions;
import org.kohsuke.github.GHBranch;

/**
 * Store local information about branch.
 *
 * @author Kanstantsin Shautsou
 * @see GitHubBranchRepository
 */
public class GitHubBranch {

    private String name;
    private String commitSha;
    private String htmlUrl;

    public GitHubBranch(GHBranch ghBranch) {
        name = ghBranch.getName();
        commitSha = ghBranch.getSHA1();
        htmlUrl = ghBranch.getOwner().getHtmlUrl().toString() + "/tree/" + name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String sha1) {
        this.commitSha = sha1;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public static String getIconFileName() {
        return Functions.getResourcePath() + "/plugin/github-pullrequest/git-branch.svg";
    }

}
