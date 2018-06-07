package com.github.kostyasha.github.integration.multibranch.scm;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import hudson.Extension;
import hudson.plugins.git.GitSCM;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * @author Kanstantsin Shautsou
 */
public class AsIsGitSCMFactory extends GitHubSCMFactory {

    private GitSCM gitSCM;

    @DataBoundConstructor
    public AsIsGitSCMFactory(GitSCM gitSCM) {
        this.gitSCM = gitSCM;
    }

    public GitSCM getGitSCM() {
        return gitSCM;
    }

    @Override
    public GitSCM createScm(GitHubSCMSource scmSource, SCMHead scmHead, SCMRevision scmRevision) {
        return new GitSCM(
                gitSCM.getUserRemoteConfigs(),
                gitSCM.getBranches(),
                gitSCM.isDoGenerateSubmoduleConfigurations(),
                gitSCM.getSubmoduleCfg(),
                gitSCM.getBrowser(),
                gitSCM.getGitTool(),
                gitSCM.getExtensions()
        );
    }

    @Symbol("asIsGITScm")
    @Extension
    public static class DescriptorImpl extends GitHubSCMFactoryDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "As Is Git SCM";
        }
    }
}
