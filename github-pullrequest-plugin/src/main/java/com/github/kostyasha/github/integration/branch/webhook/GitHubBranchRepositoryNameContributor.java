package com.github.kostyasha.github.integration.branch.webhook;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import hudson.Extension;
import hudson.model.Job;

import java.util.Collection;

import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

/**
 * Branch Trigger tied to GitHub repo. Report it for GitHub doReRegister().
 *
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubBranchRepositoryNameContributor extends GitHubRepositoryNameContributor {
    @Override
    public void parseAssociatedNames(Job<?, ?> job, Collection<GitHubRepositoryName> result) {
        final GitHubBranchTrigger gitHubBranchTrigger = ghBranchTriggerFromJob(job);
        if (nonNull(gitHubBranchTrigger)) {
            result.add(gitHubBranchTrigger.getRepoFullName(job));
        }
    }
}
