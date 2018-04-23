package com.github.kostyasha.github.integration.multibranch;

import com.cloudbees.hudson.plugins.folder.computed.ComputedFolder;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubBranchHandler;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubHandler;
import hudson.model.BuildableItem;
import hudson.model.Item;
import hudson.model.Job;
import hudson.triggers.Trigger;
import jenkins.branch.MultiBranchProject;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;

import java.util.function.Predicate;

import static org.jenkinsci.plugins.github.util.JobInfoHelpers.triggerFrom;

/**
 * @author Kanstantsin Shautsou
 */
public class Functions {
    private Functions() {
    }

    public static <ITEM extends Item> Predicate<ITEM> withTrigger(final Class<? extends Trigger> clazz) {
        return item -> triggerFrom(item, clazz) != null;
    }

    /**
     * Can be useful to ignore disabled jobs on reregistering hooks
     *
     * @return predicate with true on apply if item is buildable
     */
    public static <ITEM extends Item> Predicate<ITEM> isBuildable() {
        return item -> {
            if (item instanceof Job) {
                return ((Job) item).isBuildable();
            } else if (item instanceof ComputedFolder) {
                return ((ComputedFolder) item).isBuildable();
            }

            return item instanceof BuildableItem;
        };
    }

    public static <ITEM extends Item> Predicate<ITEM> withBranchHandler() {
        return item -> {
            if (item instanceof SCMSourceOwner) {
                SCMSourceOwner scmSourceOwner = (SCMSourceOwner) item;
                for (SCMSource source : scmSourceOwner.getSCMSources()) {
                    if (source instanceof GitHubSCMSource) {
                        GitHubSCMSource gitHubSCMSource = (GitHubSCMSource) source;
                        for (GitHubHandler hubHandler : gitHubSCMSource.getHandlers()) {
                            if (hubHandler instanceof GitHubBranchHandler) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        };
    }
//
//    public static <PROJECT extends MultiBranchProject> Predicate<PROJECT> withGitHubSCMSource(String repo) {
//        return project -> {
//
//        };
//    }

    public static <PROJECT extends MultiBranchProject> Predicate<PROJECT> withGitHubSCMSource() {
        return project -> project.getSCMSources().stream()
                .anyMatch(p -> p instanceof GitHubSCMSource);
    }
}
