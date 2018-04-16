package com.github.kostyasha.github.integration.multibranch.scm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.GitSCM;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCMRevisionState;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Kanstantsin Shautsou
 */
public class NoGitSCM extends GitSCM {

    public NoGitSCM(String repositoryUrl) {
        super(repositoryUrl);
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return null;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, Map<String, String> env) {
    }

    @Override
    public void buildEnvironment(Run<?, ?> build, Map<String, String> env) {
    }

    @Override
    public void checkout(Run<?, ?> build, Launcher launcher, FilePath workspace, TaskListener listener, File changelogFile, SCMRevisionState baseline) throws IOException, InterruptedException {
        listener.getLogger().println("No SCM checkout.");
    }
}
