package com.github.kostyasha.github.integration.branch;

import hudson.Functions;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRepository;

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
        this(ghBranch.getName(), ghBranch.getSHA1(), ghBranch.getOwner());
    }

    public GitHubBranch(String name, String commitSha, GHRepository ghRepository) {
        this.name = name;
        this.commitSha = commitSha;
        this.htmlUrl = ghRepository.getHtmlUrl().toString() + "/tree/" + name;
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
