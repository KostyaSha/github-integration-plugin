package com.github.kostyasha.github.integration.generic.errors;

import hudson.model.ProminentProjectAction;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.synchronizedSet;
import static java.util.Objects.isNull;

/**
 * Action that reports errors on job view page. Consists of sinle Actions.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubErrorsAction implements ProminentProjectAction {

    private String description;
    private final Set<GitHubError> errors = synchronizedSet(new HashSet<>());

    public GitHubErrorsAction(@Nonnull String description) {
        this.description = description;
    }

    public boolean hasVisibleErrors() {
        boolean hasVisible = false;
        for (GitHubError error : errors) {
            if (error.isVisible()) {
                hasVisible = true;
                break;
            }
        }
        return hasVisible;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    @Nonnull
    public Set<GitHubError> getErrors() {
        return errors;
    }

    public boolean addOrReplaceError(@Nonnull GitHubError a) {
        if (isNull(a)) {
            throw new IllegalArgumentException("Action must be non-null");
        }
        Set<GitHubError> old = new HashSet<>(1);
        Set<GitHubError> current = getErrors();
        boolean found = false;
        for (GitHubError curErr : current) {
            if (!found && a.equals(curErr)) {
                found = true;
            } else  if (curErr.getClass() == a.getClass()) {
                old.add(curErr);
            }
        }
        current.removeAll(old);
        if (!found) {
            getErrors().add(a);
        }
        return !found || !old.isEmpty();
    }

    public boolean removeErrors(@Nonnull Class<? extends GitHubError> clazz) {
        if (isNull(clazz)) {
            throw new IllegalArgumentException("Action type must be non-null");
        }
        Set<GitHubError> old = new HashSet<>();
        Set<GitHubError> current = getErrors();
        for (GitHubError err : current) {
            if (clazz.isInstance(err)) {
                old.add(err);
            }
        }
        return current.removeAll(old);
    }

    @Nonnull
    public Set<GitHubError> getErrorsSnapshot() {
        synchronized (errors) {
            return new HashSet<>(getErrors());
        }
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    public String getUrlName() {
        return "";
    }
}
