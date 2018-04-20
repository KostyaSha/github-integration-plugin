package com.github.kostyasha.github.integration.branch.events.impl.commitchecks;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * @see GitHubBranchCommitCheck
 */
public abstract class GitHubBranchCommitCheckDescriptor extends Descriptor<GitHubBranchCommitCheck> {
    public static DescriptorExtensionList<GitHubBranchCommitCheck, GitHubBranchCommitCheckDescriptor> all() {
        return Jenkins.getInstance().getDescriptorList(GitHubBranchCommitCheck.class);
    }
}
