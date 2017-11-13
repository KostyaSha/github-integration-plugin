package com.github.kostyasha.github.integration.multibranch.action;

import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import hudson.model.Action;
import hudson.model.Saveable;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.net.URL;

/**
 * Action as storage of critical (and not) information required for triggering decision.
 * Like {@link org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository}
 * and {@link com.github.kostyasha.github.integration.branch.GitHubBranchRepository}, but will contain
 * anything related to repo in this single class.
 */
public class GitHubRepo implements Action {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubRepo.class);

    private transient Action owner;

    private GitHubBranchRepository branchRepository;
    private GitHubPRRepository prRepository;


    public GitHubRepo() {
    }

    /**
     * When remote side available.
     */
    public GitHubRepo(GHRepository repository) {
        branchRepository = new GitHubBranchRepository(repository);
        prRepository = new GitHubPRRepository(repository);
    }

    /**
     * For offline initialisation from what user specified.
     */
    public GitHubRepo(String repoFullName, URL url) {
        branchRepository = new GitHubBranchRepository(repoFullName, url);
        prRepository = new GitHubPRRepository(repoFullName, url);
    }

    public GitHubBranchRepository getBranchRepository() {
        return branchRepository;
    }

    public GitHubPRRepository getPrRepository() {
        return prRepository;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "GitHub Local State";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }
}
