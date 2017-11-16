package com.github.kostyasha.github.integration.tag;

import com.github.kostyasha.github.integration.generic.GitHubCause;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubTagCause extends GitHubCause<GitHubTagCause> {
    @Override
    public String getShortDescription() {
        return "Tag cause";
    }
}
