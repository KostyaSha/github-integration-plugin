package com.github.kostyasha.github.integration.multibranch.action;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import hudson.BulkChange;
import hudson.Functions;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import jenkins.branch.MultiBranchProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single Action for every MultiBranch
 */
public class GitHubSCMSourcesReposAction implements Saveable, Action {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubSCMSourcesReposAction.class);

    public static final String FILE = "github-repositories.runtime.xml";

    @Nonnull
    protected transient MultiBranchProject<?, ?> project;

    protected Map<String, GitHubRepo> repoStates = new ConcurrentHashMap<>();

    public GitHubRepo getOrCreate(final GitHubSCMSource source) {
        return repoStates.computeIfAbsent(source.getId(),
                s -> new GitHubRepo(this)
        );
    }

    public Map<String, GitHubRepo> getRepoStates() {
        return repoStates;
    }

    public GitHubSCMSourcesReposAction setRepoStates(Map<String, GitHubRepo> repoStates) {
        this.repoStates = repoStates;
        return this;
    }

    public GitHubSCMSourcesReposAction(@Nonnull MultiBranchProject<?, ?> project) {
        this.project = project;
    }

    public MultiBranchProject<?, ?> getProject() {
        return project;
    }

    public void setJob(MultiBranchProject<?, ?> project) {
        this.project = project;
    }

    public XmlFile getConfigFile() {
        return new XmlFile(new File(project.getRootDir(), FILE));
    }

    public void saveQuietly() {
        try {
            save();
        } catch (IOException e) {
            LOG.error("Can't save repository state, because: '{}'", e.getMessage(), e);
        }
    }

    @Override
    public void save() throws IOException {
        synchronized (this) {
            if (BulkChange.contains(this)) {
                return;
            }

            getConfigFile().write(this);
        }
        SaveableListener.fireOnChange(this, getConfigFile());
    }


    public synchronized void load() throws IOException {
        if (getConfigFile().exists()) {
            getConfigFile().unmarshal(this);
        }
        repoStates.values().forEach(v -> v.setOwner(this));
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return Functions.getResourcePath() + "/plugin/github-pullrequest/git-pull-request.svg";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "GitHub Repos";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "github-repos";
    }

}
