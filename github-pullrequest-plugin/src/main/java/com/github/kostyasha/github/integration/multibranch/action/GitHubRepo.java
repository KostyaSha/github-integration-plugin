package com.github.kostyasha.github.integration.multibranch.action;

import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.tag.GitHubTagRepository;
import hudson.model.Action;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.net.URL;

import static java.util.Objects.isNull;

/**
 * Action as storage of critical (and not) information required for triggering decision.
 * Like {@link org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository}
 * and {@link com.github.kostyasha.github.integration.branch.GitHubBranchRepository}, but will contain
 * anything related to repo in this single class.
 */
public class GitHubRepo implements Action {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubRepo.class);

    private transient GitHubSCMSourcesLocalStorage owner;

    private GitHubBranchRepository branchRepository;
    private GitHubTagRepository tagRepository;
    private GitHubPRRepository prRepository;

    public GitHubRepo(GitHubSCMSourcesLocalStorage owner) {
        this.owner = owner;
    }

    /**
     * When remote side available.
     */
    public GitHubRepo(GHRepository repository) throws IOException {
        this(new GitHubBranchRepository(repository), new GitHubTagRepository(repository), new GitHubPRRepository(repository));
    }

    /**
     * For offline initialisation from what user specified.
     */
    public GitHubRepo(String repoFullName, URL url) {
        this(new GitHubBranchRepository(repoFullName, url), new GitHubTagRepository(repoFullName, url), new GitHubPRRepository(repoFullName, url));
    }

    public GitHubRepo(GitHubBranchRepository branchRepository, GitHubTagRepository tagRepository, GitHubPRRepository prRepository) {
        this.branchRepository = branchRepository;
        this.tagRepository = tagRepository;
        this.prRepository = prRepository;
    }

    public GitHubBranchRepository getBranchRepository() {
        return branchRepository;
    }

    public GitHubTagRepository getTagRepository() {
        return tagRepository;
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

    public void actualize(GHRepository remoteRepo) throws IOException {
        if (isNull(branchRepository)) {
            branchRepository = new GitHubBranchRepository(remoteRepo);
        } else {
            branchRepository.actualise(remoteRepo, TaskListener.NULL);
        }

        if (isNull(tagRepository)) {
            tagRepository = new GitHubTagRepository(remoteRepo);
        } else  {
            tagRepository.actualise(remoteRepo, TaskListener.NULL);
        }
        if (isNull(prRepository)) {
            prRepository = new GitHubPRRepository(remoteRepo);
        } else {
            prRepository.actualise(remoteRepo, TaskListener.NULL);
        }

        if (owner != null) {
            owner.save();
        }
    }

    public void sync(GHRepository remoteRepo) {

    }

    public void setOwner(GitHubSCMSourcesLocalStorage owner) {
        this.owner = owner;
    }


}
