package org.jenkinsci.plugins.github.pullrequest;

import com.google.common.base.Joiner;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Label object that contains user defined labels
 *
 * @author Alina Karpovich
 */
public class GitHubPRLabel implements Describable<GitHubPRLabel> {
    private Set<String> labels;

    @DataBoundConstructor
    public GitHubPRLabel(String labels) {
        this.labels = new HashSet<String>(Arrays.asList(labels.split("\n")));
    }

    //for UI binding
    public String getLabels() {
        return Joiner.on("\n").skipNulls().join(labels);
    }

    @Nonnull
    public Set<String> getLabelsSet() {
        return (labels != null) ? labels : Collections.<String>emptySet();
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptor(GitHubPRLabel.class);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GitHubPRLabel> {
        @Override
        public String getDisplayName() {
            return "Labels";
        }
    }
}
