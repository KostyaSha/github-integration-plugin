package com.github.kostyasha.github.integration.branch.utils;

import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import hudson.model.Item;
import hudson.model.Job;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;

import javax.annotation.CheckForNull;
import java.util.function.Predicate;

import static java.util.Objects.nonNull;

public class ItemHelpers {
    private ItemHelpers() {
    }


    public static Predicate<Item> isBuildable() {
        return item -> nonNull(item) && item instanceof Job && ((Job) item).isBuildable();
    }


    @CheckForNull
    public static GitHubBranchRepository getBranchRepositoryFor(Item item) {
        if (item instanceof Job) {
            Job<?, ?> job = (Job) item;
            return job.getAction(GitHubBranchRepository.class);
        }

        return null;
    }

    @CheckForNull
    public static GitHubPRRepository getPRRepositoryFor(Item item) {
        if (item instanceof Job) {
            Job<?, ?> job = (Job) item;
            return job.getAction(GitHubPRRepository.class);
        }

        return null;
    }
}
