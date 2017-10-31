package com.github.kostyasha.github.integration.multibranch;

import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import jenkins.branch.MultiBranchProject;
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
public class GitHubRepo implements Action, Saveable {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubRepo.class);

    private final GitHubBranchRepository branchRepository;
    private final GitHubPRRepository prRepository;

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


    @Override
    public void save() throws IOException {

    }
}
