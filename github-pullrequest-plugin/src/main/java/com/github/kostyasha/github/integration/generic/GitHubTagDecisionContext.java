package com.github.kostyasha.github.integration.generic;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubTagHandler;
import com.github.kostyasha.github.integration.tag.GitHubTag;
import com.github.kostyasha.github.integration.tag.GitHubTagRepository;

import hudson.model.TaskListener;
import org.kohsuke.github.GHTag;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubTagDecisionContext extends GitHubDecisionContext {

    private final GHTag remoteTag;
    private final GitHubTag localTag;
    private final GitHubTagRepository localRepo;
    private final GitHubTagHandler tagHandler;
    private final GitHubSCMSource scmSource;

    public GitHubTagDecisionContext(GHTag remoteTag, GitHubTag localTag, @Nonnull GitHubTagRepository localRepo, GitHubTagHandler tagHandler, GitHubSCMSource scmSource, TaskListener listener) {
        super(listener);
        this.remoteTag = remoteTag;
        this.localTag = localTag;
        this.localRepo = localRepo;
        this.tagHandler = tagHandler;
        this.scmSource = scmSource;
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

    public GitHubTagHandler getTagHandler() {
        return tagHandler;
    }

    public GitHubSCMSource getScmSource() {
        return scmSource;
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
