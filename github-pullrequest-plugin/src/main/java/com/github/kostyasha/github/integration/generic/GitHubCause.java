package com.github.kostyasha.github.integration.generic;

import com.github.kostyasha.github.integration.multibranch.head.GitHubSCMHead;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
import hudson.model.Cause;
import hudson.model.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.lang.StringUtils.abbreviate;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubCause<T extends GitHubCause<T>> extends Cause {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubCause.class);

    private boolean skip;
    private String reason;

    /**
     * Doesn't exist for deleted branch.
     */
    @CheckForNull
    private URL htmlUrl;

    @CheckForNull
    private String title;

    private String gitUrl;
    private String sshUrl;

    private String pollingLog;

    private transient Object remoteData;

    public GitHubCause withLocalRepo(@Nonnull GitHubRepository localRepo) {
        withGitUrl(localRepo.getGitUrl());
        withSshUrl(localRepo.getSshUrl());
        withHtmlUrl(localRepo.getGithubUrl());
        return this;
    }

    /**
     * When set and event got positive condition says to skip job triggering.
     */
    public boolean isSkip() {
        return skip;
    }

    public GitHubCause<T> withSkip(boolean skip) {
        this.skip = skip;
        return this;
    }

    /**
     * For printing branch url on left builds panel (build description).
     */
    @CheckForNull
    public URL getHtmlUrl() {
        return htmlUrl;
    }

    public GitHubCause<T> withHtmlUrl(URL htmlUrl) {
        this.htmlUrl = htmlUrl;
        return this;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public GitHubCause<T> withGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
        return this;
    }

    public String getSshUrl() {
        return sshUrl;
    }

    public GitHubCause<T> withSshUrl(String sshUrl) {
        this.sshUrl = sshUrl;
        return this;
    }

    public String getPollingLog() {
        return pollingLog;
    }

    public GitHubCause<T> withPollingLog(String pollingLog) {
        this.pollingLog = pollingLog;
        return this;
    }

    public void setPollingLog(String pollingLog) {
        this.pollingLog = pollingLog;
    }

    public void setPollingLogFile(File logFile) throws IOException {
        this.pollingLog = readFileToString(logFile);
    }

    public String getReason() {
        return reason;
    }

    public GitHubCause<T> withReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Object getRemoteData() {
        return remoteData;
    }

    public GitHubCause<T> withRemoteData(Object remoteData) {
        this.remoteData = remoteData;
        return this;
    }

    /**
     * @return the title of the cause, never null.
     */
    @Nonnull
    public String getTitle() {
        return nonNull(title) ? title : "";
    }

    public GitHubCause<T> withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * @return at most the first 30 characters of the title, or
     */
    public String getAbbreviatedTitle() {
        return abbreviate(getTitle(), 30);
    }

    public abstract void fillParameters(List<ParameterValue> params);

    public abstract GitHubSCMHead<T> createSCMHead(String sourceId);

    @SuppressWarnings("unchecked")
    public GitHubSCMRevision createSCMRevision(String sourceId) {
        return createSCMHead(sourceId).createSCMRevision((T) this).setRemoteData(getRemoteData());
    }

    public static <T extends GitHubCause<T>> T skipTrigger(List<? extends T> causes) {
        if (isNull(causes)) {
            return null;
        }
        T cause = causes.stream()
                .filter(GitHubCause::isSkip)
                .findFirst()
                .orElse(null);

        return cause;
    }
}
