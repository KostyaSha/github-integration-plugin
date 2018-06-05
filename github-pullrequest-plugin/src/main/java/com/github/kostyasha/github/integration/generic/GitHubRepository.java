package com.github.kostyasha.github.integration.generic;

import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Saveable;
import hudson.model.TaskListener;
import hudson.model.listeners.SaveableListener;
import hudson.util.FormValidation;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

import static java.util.Objects.isNull;

/**
 * Action as storage of critical (and not) information required for triggering decision.
 *
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

    public GitHubRepository(@Nonnull GHRepository ghRepository) throws IOException {
        actualise(ghRepository, TaskListener.NULL);
    }

    public GitHubRepository(String repoFullName, URL githubUrl) {
        this.fullName = repoFullName;
        this.githubUrl = githubUrl;
    }

    /**
     * Repository may be created without gh connection, but trigger logic expects this fields.
     * Should be called before trigger logic starts checks.
     */
    public void actualise(@Nonnull GHRepository ghRepository, TaskListener listener) throws IOException {
        boolean changed = false;

        PrintStream logger = listener.getLogger();
        // just in case your organisation decided to change domain
        // take into account only repo/name
        if (isNull(fullName) || !fullName.equals(ghRepository.getFullName())) {
            logger.printf("Repository full name changed '%s' to '%s'.", fullName, ghRepository.getFullName());
            fullName = ghRepository.getFullName();
            changed = true;
        }

        if (isNull(githubUrl) || !githubUrl.equals(ghRepository.getHtmlUrl())) {
            logger.printf("Changing GitHub url from '%s' to '%s'.", githubUrl, ghRepository.getHtmlUrl());
            githubUrl = ghRepository.getHtmlUrl();
        }

        if (isNull(gitUrl) || !gitUrl.equals(ghRepository.getGitTransportUrl())) {
            logger.printf("Changing Git url from '%s' to '%s'.", gitUrl, ghRepository.getGitTransportUrl());
            gitUrl = ghRepository.getGitTransportUrl();
        }

        if (isNull(sshUrl) || !sshUrl.equals(ghRepository.getSshUrl())) {
            logger.printf("Changing SSH url from '%s' to '%s'.", sshUrl, ghRepository.getSshUrl());
            sshUrl = ghRepository.getSshUrl();
        }

        if (changed) {
            logger.println("Full name changed, removing branches in repository!");
            ghRepository.getBranches().clear();
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
    public void save() throws IOException {
        synchronized (this) {
            if (BulkChange.contains(this)) {
                return;
            }

            configFile.write(this);
        }
        SaveableListener.fireOnChange(this, configFile);
    }

    public abstract FormValidation doClearRepo() throws IOException;

    public abstract FormValidation doRunTrigger() throws IOException;

    public abstract FormValidation doRebuildAllFailed() throws IOException;

    /**
     * Build using local PR state.
     */
    public abstract FormValidation doBuild(StaplerRequest req) throws IOException;

    /**
     * Rebuild latest built build. Actions copied.
     */
    public abstract FormValidation doRebuild(StaplerRequest req) throws IOException;

}
