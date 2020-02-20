package com.github.kostyasha.github.integration.multibranch.hooks;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import hudson.scm.SCM;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMSource;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Objects;

import static java.util.Objects.*;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubScmHeadEvent<T> extends SCMHeadEvent<T> {

    protected GitHubScmHeadEvent(Type type, long timestamp, T payload, String origin) {
        super(type, timestamp, payload, origin);
    }

    @Nonnull
    protected abstract String getSourceRepo();

    @Override
    public boolean isMatch(@Nonnull SCMSource source) {
        if (!(source instanceof GitHubSCMSource)) {
            return false;
        }
        return getSourceRepo().equals(getSourceRepo(source));
    }

    @Nonnull
    @Override
    public String getSourceName() {
        return getSourceRepo();
    }

    @CheckForNull
    protected String getSourceRepo(@Nonnull SCMSource source) {
        GitHubSCMSource gitHubSCMSource = (GitHubSCMSource) source;
        String projectUrlStr = gitHubSCMSource.getProjectUrlStr();
        GitHubRepositoryName repo = GitHubRepositoryName.create(projectUrlStr);
        return isNull(repo) ? null : String.format("%s/%s", repo.getUserName(), repo.getRepositoryName());
    }

    @Override
    public boolean isMatch(SCMNavigator navigator) {
        return false;
    }

    @Override
    public boolean isMatch(SCM scm) {
        return false;
    }
}
