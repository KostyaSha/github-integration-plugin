package com.github.kostyasha.github.integration.tag;

import org.kohsuke.github.GHTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * @author Kanstantsin Shautsou
 */
public class LocalRepoUpdater implements Function<GHTag, GHTag> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRepoUpdater.class);
    private final GitHubTagRepository localRepo;

    private LocalRepoUpdater(GitHubTagRepository localRepo) {
        this.localRepo = localRepo;
    }

    public static LocalRepoUpdater updateLocalRepo(GitHubTagRepository localRepo) {
        return new LocalRepoUpdater(localRepo);
    }

    @Override
    public GHTag apply(GHTag remoteTag) {
        LOGGER.trace("Updating local branch repository with [{}]", remoteTag.getName());
        localRepo.getTags().put(remoteTag.getName(), new GitHubTag(remoteTag));

        return remoteTag;
    }
}
