package org.jenkinsci.plugins.github.pullrequest.webhook;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import hudson.Extension;
import hudson.model.Job;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;

import java.util.Collection;

import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromJob;
import static java.util.Objects.nonNull;

/**
 * PR Trigger tied to GitHub repo. Report it for GitHub doReRegister().
 *
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubPRRepositoryNameContributor extends GitHubRepositoryNameContributor {
    @Override
    public void parseAssociatedNames(Job<?, ?> job, Collection<GitHubRepositoryName> result) {
        final GitHubPRTrigger gitHubPRTrigger = ghPRTriggerFromJob(job);
        if (nonNull(gitHubPRTrigger)) {
            result.add(gitHubPRTrigger.getRepoFullName(job));
        }
    }
}
