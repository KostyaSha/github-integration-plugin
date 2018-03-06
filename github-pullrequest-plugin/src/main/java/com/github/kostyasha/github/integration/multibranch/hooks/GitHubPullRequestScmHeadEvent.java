package com.github.kostyasha.github.integration.multibranch.hooks;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.head.GitHubPRSCMHead;

import hudson.scm.SCM;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.github.pullrequest.webhook.PullRequestInfo;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPullRequestScmHeadEvent extends SCMHeadEvent<PullRequestInfo> {
    public GitHubPullRequestScmHeadEvent(@Nonnull Type type,
                                    long timestamp,
                                    @Nonnull PullRequestInfo payload,
                                    @CheckForNull String origin) {
        super(type, timestamp, payload, origin);
    }

    @Override
    public boolean isMatch(@Nonnull SCMNavigator navigator) {
        return false;
    }

    @Nonnull
    @Override
    public String getSourceName() {
        return getPayload().getRepo();
    }

    @Nonnull
    @Override
    public Map<SCMHead, SCMRevision> heads(@Nonnull SCMSource source) {
        if (!(source instanceof GitHubSCMSource)) {
            return Collections.emptyMap();
        }

        GitHubSCMSource gitHubSCMSource = (GitHubSCMSource) source;
        String projectUrlStr = gitHubSCMSource.getProjectUrlStr();
        GitHubRepositoryName repo = GitHubRepositoryName.create(projectUrlStr);
        String sourceRepo = String.format("%s/%s", repo.getUserName(), repo.getRepositoryName());

        if (sourceRepo.equals(getPayload().getRepo())) {
            HashMap<SCMHead, SCMRevision> heads = new HashMap<>(1);
            heads.put(
                    new GitHubPRSCMHead(Integer.toString(getPayload().getNum()), source.getId()),
                    null
            );
            return heads;
        }

        return Collections.emptyMap();
    }

    @Override
    public boolean isMatch(@Nonnull SCM scm) {
        return false;
    }
}
