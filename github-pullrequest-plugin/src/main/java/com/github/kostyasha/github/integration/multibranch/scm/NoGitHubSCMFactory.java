package com.github.kostyasha.github.integration.multibranch.scm;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import hudson.Extension;
import hudson.plugins.git.GitSCM;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Kanstantsin Shautsou
 */
public class NoGitHubSCMFactory extends GitHubSCMFactory {

    @DataBoundConstructor
    public NoGitHubSCMFactory() {
    }

    @Override
    public GitSCM createScm(GitHubSCMSource scmSource, SCMHead scmHead, SCMRevision scmRevision) {
        return new NoGitSCM(null);
    }

    @Symbol("noGITScm")
    @Extension
    public static class DescriptorImpl extends GitHubSCMFactoryDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return "No SCM";
        }
    }
}
