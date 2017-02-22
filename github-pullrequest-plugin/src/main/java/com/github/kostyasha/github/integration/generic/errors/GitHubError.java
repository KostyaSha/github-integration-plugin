package com.github.kostyasha.github.integration.generic.errors;

import hudson.ExtensionPoint;

/**
 * Custom errors that participate in list of {@link GitHubErrorsAction}.
 * index.groovy
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubError implements ExtensionPoint {
    public abstract String getTitle();
    public abstract String getHtmlDescription();
}
