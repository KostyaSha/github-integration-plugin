package com.github.kostyasha.github.integration.multibranch.action;

import com.github.kostyasha.github.integration.tag.GitHubTag;
import org.kohsuke.github.GHRepository;

/**
 * Add a link to repository branch
 *
 * @author Anton Tanasenko
 */
public class GitHubTagAction extends GitHubLinkAction {

    private String tag;

    public GitHubTagAction(GHRepository remoteRepository, String tag) {
        super(buildUrl(remoteRepository, tag));
        this.tag = tag;
    }

    @Override
    public String getDisplayName() {
        return "Tag " + tag;
    }

    @Override
    public String getIconFileName() {
        return GitHubTag.getIconFileName();
    }

    private static String buildUrl(GHRepository remoteRepository, String tag) {
        String repoUrl = remoteRepository.getHtmlUrl().toExternalForm();
        if (remoteRepository.getDefaultBranch().equals(tag)) {
            return repoUrl;
        }
        return repoUrl + "/releases/tag/" + tag;
    }

}
