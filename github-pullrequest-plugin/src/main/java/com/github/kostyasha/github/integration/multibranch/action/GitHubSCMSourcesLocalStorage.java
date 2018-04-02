package com.github.kostyasha.github.integration.multibranch.action;

import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.PersistenceRoot;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

/**
 * Single Action for every MultiBranch
 */
public class GitHubSCMSourcesLocalStorage implements Saveable {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubSCMSourcesLocalStorage.class);

    @Nonnull
    protected transient PersistenceRoot project;

    private final String sourceId;
    private final GitHubRepo repoState;

    public GitHubSCMSourcesLocalStorage(@Nonnull PersistenceRoot project, @Nonnull String sourceId) {
        this.project = project;
        this.sourceId = sourceId;
        repoState = new GitHubRepo(this);
    }

    public GitHubRepo getLocalRepo() {
        return repoState;
    }

    public XmlFile getConfigFile() {
        return new XmlFile(new File(project.getRootDir(), "github-scm-" + normalizedSourceId() + ".xml"));
    }

    private String normalizedSourceId() {
        return DigestUtils.sha1Hex(sourceId);
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
        repoState.setOwner(this);
    }

}
