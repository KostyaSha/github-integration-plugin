package com.github.kostyasha.github.integration.tag;

import hudson.Functions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTag;

import com.github.kostyasha.github.integration.branch.GitHubBranch;

/**
 * Store local information about tag.
 *
 * @author Kanstantsin Shautsou
 * @see GitHubTagRepository
 */
public class GitHubTag extends GitHubBranch {

    public GitHubTag(GHTag ghTag) {
        this(ghTag.getName(), ghTag.getCommit().getSHA1(), ghTag.getOwner());
    }

    public GitHubTag(String name, String commitSha, GHRepository ghRepository) {
        super(name, commitSha, ghRepository);
    }

    public static String getIconFileName() {
        return Functions.getResourcePath() + "/plugin/github-pullrequest/git-tag.svg";
    }

    public static GHTag findRemoteTag(GHRepository repo, String name) throws IOException {
        Stream<GHTag> tagStream = StreamSupport.stream(repo.listTags().spliterator(), false);
        return tagStream.filter(t -> t.getName().equals(name)).findFirst().orElse(null);
    }

    public static List<GHTag> getAllTags(GHRepository repo) throws IOException {
        List<GHTag> tags = new ArrayList<>();
        repo.listTags().forEach(tags::add);
        return tags;
    }
}
