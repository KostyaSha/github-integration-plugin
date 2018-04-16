package com.github.kostyasha.github.integration.multibranch.scm;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.plugins.git.GitSCM;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;

public abstract class GitHubSCMFactory extends AbstractDescribableImpl<GitHubSCMFactory>
        implements ExtensionPoint {

    public abstract GitSCM createScm(GitHubSCMSource scmSource, SCMHead scmHead, SCMRevision scmRevision);

    public abstract static class GitHubSCMFactoryDescriptor extends Descriptor<GitHubSCMFactory> {
        public static DescriptorExtensionList<GitHubSCMFactory, GitHubSCMFactoryDescriptor> getGitHubSCMFactoryDescriptors() {
            return Jenkins.getInstance().getDescriptorList(GitHubSCMFactory.class);
        }
    }
}
