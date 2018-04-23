package com.github.kostyasha.github.integration.multibranch.hooks;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import hudson.Extension;
import hudson.model.Item;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;

import java.util.Collection;

/**
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubScmSourceRepositoryNameContributor extends GitHubRepositoryNameContributor {
    @Override
    public void parseAssociatedNames(Item item, Collection<GitHubRepositoryName> result) {
        if (item instanceof SCMSourceOwner) {
            SCMSourceOwner sourceOwner = (SCMSourceOwner) item;
            for (SCMSource source : sourceOwner.getSCMSources()) {
                if (source instanceof GitHubSCMSource) {
                    GitHubSCMSource gitHubSCMSource = (GitHubSCMSource) source;
                    result.add(gitHubSCMSource.getRepoFullName());
                }
            }
        }
    }
}
