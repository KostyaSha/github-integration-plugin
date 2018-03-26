package com.github.kostyasha.github.integration.generic;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubTagHandler;
import com.github.kostyasha.github.integration.tag.GitHubTag;
import com.github.kostyasha.github.integration.tag.GitHubTagCause;
import com.github.kostyasha.github.integration.tag.GitHubTagRepository;
import com.github.kostyasha.github.integration.tag.events.GitHubTagEvent;

import hudson.model.TaskListener;
import org.kohsuke.github.GHTag;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubTagDecisionContext extends GitHubDecisionContext<GitHubTagEvent, GitHubTagCause> {

    private final GHTag remoteTag;
    private final GitHubTag localTag;
    private final GitHubTagRepository localRepo;

    public GitHubTagDecisionContext(GHTag remoteTag, GitHubTag localTag, @Nonnull GitHubTagRepository localRepo, GitHubTagHandler tagHandler, GitHubSCMSource scmSource, TaskListener listener) {
        super(listener, null, scmSource, tagHandler);
        this.remoteTag = remoteTag;
        this.localTag = localTag;
        this.localRepo = localRepo;
    }

    /**
     * @return current tag state fetched from GH.
     */
    @CheckForNull
    public GHTag getRemoteTag() {
        return remoteTag;
    }

    /**
     * @return tag state from last run saved in jenkins. null when not exist before.
     */
    @CheckForNull
    public GitHubTag getLocalTag() {
        return localTag;
    }

    /**
     * @return local repository state. Useful to extract repo URLs for example.
     */
    @Nonnull
    public GitHubTagRepository getLocalRepo() {
        return localRepo;
    }

    @Override
    public GitHubTagHandler getHandler() {
        return (GitHubTagHandler) super.getHandler();
    }
    
    @Override
    public GitHubTagCause checkEvent(GitHubTagEvent event) throws IOException {
        return event.check(this);
    }

    public static class Builder {
        private GHTag remoteTag = null;
        private GitHubTag localTag = null;
        private TaskListener listener;

        // depends on what job type it used
        private GitHubTagHandler tagHandler = null;
        private GitHubSCMSource scmSource;

        private GitHubTagRepository localRepo;

        public Builder() {}

        public Builder withRemoteTag(@CheckForNull GHTag remoteTag) {
            this.remoteTag = remoteTag;
            return this;
        }

        public Builder withLocalTag(@CheckForNull GitHubTag localTag) {
            this.localTag = localTag;
            return this;
        }

        public Builder withLocalRepo(GitHubTagRepository localRepo) {
            this.localRepo = localRepo;
            return this;
        }

        public Builder withListener(@Nonnull TaskListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder withTagHandler(@CheckForNull GitHubTagHandler tagHandler) {
            this.tagHandler = tagHandler;
            return this;
        }

        // TODO abstract?
        public Builder withSCMSource(GitHubSCMSource scmSource) {
            this.scmSource = scmSource;
            return this;
        }


        public GitHubTagDecisionContext build() {
            requireNonNull(tagHandler);
            requireNonNull(scmSource);
            requireNonNull(listener);

            return new GitHubTagDecisionContext(remoteTag, localTag, localRepo, tagHandler, scmSource, listener);
        }

    }

    @Nonnull
    public static Builder newGitHubTagDecisionContext() {
        return new Builder();
    }
}
