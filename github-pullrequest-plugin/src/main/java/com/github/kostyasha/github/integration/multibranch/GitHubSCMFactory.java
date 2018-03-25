package com.github.kostyasha.github.integration.multibranch;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.plugins.git.GitSCM;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;

public abstract class GitHubSCMFactory implements Describable<GitHubSCMFactory>, ExtensionPoint {

    public abstract GitSCM createScm(GitHubSCMSource scmSource, SCMHead scmHead, SCMRevision scmRevision);

    public static DescriptorExtensionList<GitHubSCMFactory, GitHubSCMFactoryDescriptor> all() {
        return Jenkins.getInstance().getDescriptorList(GitHubSCMFactory.class);
    }

    public abstract class GitHubSCMFactoryDescriptor extends Descriptor<GitHubSCMFactory> {

    }

}
