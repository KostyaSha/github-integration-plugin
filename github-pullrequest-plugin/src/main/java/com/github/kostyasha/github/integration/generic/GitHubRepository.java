package com.github.kostyasha.github.integration.generic;

import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.util.FormValidation;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;

import static java.util.Objects.isNull;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubRepository<T extends GitHubRepository> implements Action, Saveable {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubRepository.class);

    protected transient XmlFile configFile; // for save()
    protected transient Job<?, ?> job;  // for UI

    @CheckForNull
    private String fullName;
    @CheckForNull
    private URL githubUrl;

    @CheckForNull
    private String gitUrl;
    @CheckForNull
    private String sshUrl;

    public GitHubRepository(@Nonnull GHRepository ghRepository) {
        actualise(ghRepository);
    }

    public GitHubRepository(String repoFullName, URL githubUrl) {
        this.fullName = repoFullName;
        this.githubUrl = githubUrl;
    }

    /**
     * Repository may be created without gh connection, but trigger logic expects this fields.
     * Should be called before trigger logic starts checks.
     */
    public void actualise(@Nonnull GHRepository ghRepository) {
        if (isNull(fullName)) {
            fullName = ghRepository.getFullName();
        }
        if (isNull(githubUrl)) {
            githubUrl = ghRepository.getHtmlUrl();
        }
        if (isNull(gitUrl)) {
            gitUrl = ghRepository.getGitTransportUrl();
        }
        if (isNull(sshUrl)) {
            sshUrl = ghRepository.getSshUrl();
        }
    }

    public String getFullName() {
        return fullName;
    }

    public GitHubRepository<T> withFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public URL getGithubUrl() {
        return githubUrl;
    }

    public GitHubRepository<T> withGithubUrl(URL githubUrl) {
        this.githubUrl = githubUrl;
        return this;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public GitHubRepository<T> withGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
        return this;
    }

    public String getSshUrl() {
        return sshUrl;
    }

    public GitHubRepository<T> withSshUrl(String sshUrl) {
        this.sshUrl = sshUrl;
        return this;
    }

    public Job<?, ?> getJob() {
        return job;
    }

    public void setJob(Job<?, ?> job) {
        this.job = job;
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
    public synchronized void save() throws IOException {
        if (BulkChange.contains(this)) {
            return;
        }

        configFile.write(this);
        SaveableListener.fireOnChange(this, configFile);
    }

    public abstract FormValidation doClearRepo() throws IOException;

    public abstract FormValidation doRunTrigger() throws IOException;

    public abstract FormValidation doRebuildFailed() throws IOException;

    public abstract FormValidation doRebuild(StaplerRequest req) throws IOException;

}
