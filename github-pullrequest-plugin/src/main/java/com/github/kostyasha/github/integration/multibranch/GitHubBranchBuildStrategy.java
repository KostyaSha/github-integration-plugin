package com.github.kostyasha.github.integration.multibranch;

import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
import hudson.Extension;
import hudson.model.TaskListener;
import jenkins.branch.BranchBuildStrategy;
import jenkins.branch.BranchBuildStrategyDescriptor;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;

public class GitHubBranchBuildStrategy extends BranchBuildStrategy {

    @DataBoundConstructor
    public GitHubBranchBuildStrategy() {
    }

    @Override
    public boolean isAutomaticBuild(SCMSource source, SCMHead head) {
        return false;
    }

    @Override
    public boolean isAutomaticBuild(SCMSource source, SCMHead head, SCMRevision revision) {
        if (!(revision instanceof GitHubSCMRevision)) {
            return false;
        }
        GitHubSCMRevision gr = (GitHubSCMRevision) revision;
        if (gr.getCause() == null) {
            return false;
        }
        return !gr.getCause().isSkip();
    }

    @Override
    public boolean isAutomaticBuild(@NonNull SCMSource source, @NonNull SCMHead head, @NonNull SCMRevision currRevision,
            SCMRevision lastBuiltRevision, SCMRevision lastSeenRevision, @NonNull TaskListener listener) {
        return isAutomaticBuild(source, head, currRevision);
    }

    @Symbol("gitHubEvents")
    @Extension
    public static class DescriptorImpl extends BranchBuildStrategyDescriptor {
        @Override
        public boolean isApplicable(SCMSourceDescriptor sourceDescriptor) {
            return sourceDescriptor.clazz == GitHubSCMSource.class;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "GitHub events based branch build strategy";
        }
    }
}
