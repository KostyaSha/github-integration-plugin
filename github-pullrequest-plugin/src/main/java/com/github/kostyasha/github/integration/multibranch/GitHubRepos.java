package com.github.kostyasha.github.integration.multibranch;

import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import jenkins.branch.MultiBranchProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;

public class GitHubRepos implements Saveable, Action {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubRepos.class);

    protected transient XmlFile configFile; // for save()
    protected transient MultiBranchProject<?, ?> project;  // for UI ?

    public MultiBranchProject<?, ?> getProject() {
        return project;
    }

    public void setJob(MultiBranchProject<?, ?> project) {
        this.project = project;
    }

    public XmlFile getConfigFile() {
        return configFile;
    }

    public void setConfigFile(XmlFile configFile) {
        this.configFile = configFile;
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

            configFile.write(this);
        }
        SaveableListener.fireOnChange(this, configFile);
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
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
