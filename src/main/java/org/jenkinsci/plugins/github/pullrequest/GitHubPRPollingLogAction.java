package org.jenkinsci.plugins.github.pullrequest;

import hudson.Functions;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.AbstractProject;
import hudson.model.Action;
import org.apache.commons.jelly.XMLOutput;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Link for project page, shows triggered polling log
 *
 * @author Alina Karpovich
 */
public class GitHubPRPollingLogAction implements Action {
    private transient AbstractProject<?, ?> project;

    public GitHubPRPollingLogAction(AbstractProject<?, ?> project) {
        this.project = project;
    }

    public AbstractProject<?,?> getOwner() {
        return project;
    }

    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    @Override
    public String getDisplayName() {
        return "GitHub PR Polling Log";
    }

    @Override
    public String getUrlName() {
        return "github-pr-polling";
    }

    public String getLog() throws IOException {
        return Util.loadFile(getLogFile());
    }

    /**
     * Writes the annotated log to the given output.
     * @since 1.350
     */
    public void writeLogTo(XMLOutput out) throws IOException {
        new AnnotatedLargeText<GitHubPRPollingLogAction>(getLogFile(),
                Charset.defaultCharset(),true,this).writeHtmlTo(0,out.asWriter());
    }

    public File getLogFile() {
        return new File(project.getRootDir(),"github-pullrequest-polling.log");
    }
}