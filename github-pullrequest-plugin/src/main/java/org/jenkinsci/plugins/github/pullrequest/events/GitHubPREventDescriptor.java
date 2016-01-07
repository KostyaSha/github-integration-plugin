package org.jenkinsci.plugins.github.pullrequest.events;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubPREventDescriptor extends Descriptor<GitHubPREvent> {
//	public boolean isApplicable(Class<? extends GitSCM> type) {
//		return true;
//	}

    public static DescriptorExtensionList<GitHubPREvent, GitHubPREventDescriptor> all() {
        return Jenkins.getInstance().getDescriptorList(GitHubPREvent.class);
    }
}