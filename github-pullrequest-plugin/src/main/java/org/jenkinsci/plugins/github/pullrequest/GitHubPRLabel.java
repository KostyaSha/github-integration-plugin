package org.jenkinsci.plugins.github.pullrequest;

import com.google.common.base.Joiner;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.nonNull;

/**
 * Label object that contains user defined labels
 *
 * @author Alina Karpovich
 */
public class GitHubPRLabel implements Describable<GitHubPRLabel> {
    private Set<String> labels;

    @DataBoundConstructor
    public GitHubPRLabel(String labels) {
        this(new HashSet<>(Arrays.asList(labels.split("\n"))));
    }

    public GitHubPRLabel(Set<String> labels) {
        this.labels = labels;
    }

    // for UI binding
    public String getLabels() {
        return Joiner.on("\n").skipNulls().join(labels);
    }

    @NonNull
    public Set<String> getLabelsSet() {
        return nonNull(labels) ? labels : Collections.<String>emptySet();
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptor(GitHubPRLabel.class);
    }

    @Symbol("labels")
    @Extension
    public static class DescriptorImpl extends Descriptor<GitHubPRLabel> {
        @NonNull
        @Override
        public String getDisplayName() {
            return "Labels";
        }
    }
}
