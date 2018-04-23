package com.github.kostyasha.github.integration.tag.events;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubTagEventDescriptor extends Descriptor<GitHubTagEvent> {
    public static DescriptorExtensionList<GitHubTagEvent, GitHubTagEventDescriptor> all() {
        return Jenkins.getInstance().getDescriptorList(GitHubTagEvent.class);
    }
}
