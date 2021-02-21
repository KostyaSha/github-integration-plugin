package com.github.kostyasha.github.integration.multibranch.handler;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;
import org.kohsuke.github.GitHub;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.stream.Stream;

import static org.jenkinsci.plugins.github.pullrequest.utils.IOUtils.forEachIo;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubHandler extends AbstractDescribableImpl<GitHubHandler> {

    public abstract void handle(@NonNull GitHubSourceContext context) throws IOException;

    protected void processCauses(GitHubSourceContext context, Stream<? extends GitHubCause<?>> causeStream) throws IOException {
        GitHub github = context.getGitHub();
        TaskListener listener = context.getListener();

        listener.getLogger().println("GitHub rate limit before check: " + github.getRateLimit());

        // run the process
        forEachIo(causeStream, context::observe);

        listener.getLogger().println("GitHub rate limit after check: " + github.getRateLimit());
    }
}
